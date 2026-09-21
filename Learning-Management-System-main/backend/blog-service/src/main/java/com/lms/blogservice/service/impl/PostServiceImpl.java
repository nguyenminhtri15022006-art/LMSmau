package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Tag;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.PostMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.TagRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.MinioService;
import com.lms.blogservice.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostServiceImpl implements PostService {

    PostRepository postRepository;
    TagRepository tagRepository;
    CommentRepository commentRepository;
    VoteRepository voteRepository;
    PostMapper postMapper;
    MinioService minioService;

    @Value("${blog.storage.thumbnail-folder:thumbnails}")
    @NonFinal
    String thumbnailFolder;

    // ===== Public =====

    @Override
    public Page<PostSummaryResponse> getPosts(Pageable pageable, PostStatus status,
                                              Long tagId, String keyword) {
        Page<Post> posts;

        if (keyword != null && !keyword.isBlank())
            posts = postRepository.findByTitleContainingIgnoreCaseAndStatus(
                    keyword, PostStatus.PUBLISHED, pageable);
        else if (tagId != null)
            posts = postRepository.findByStatusAndTagsId(PostStatus.PUBLISHED, tagId, pageable);
        else
            posts = postRepository.findByStatus(PostStatus.PUBLISHED, pageable);

        return posts.map(this::toSummaryWithCounts);
    }

    // ===== Admin =====

    @Override
    public Page<PostSummaryResponse> getAllPostsForAdmin(Pageable pageable, PostStatus status,
                                                         Long tagId, String keyword) {
        Page<Post> posts;

        if (keyword != null && !keyword.isBlank()) {
            if (tagId != null)
                posts = postRepository.findByTitleContainingIgnoreCaseAndTagsId(keyword, tagId, pageable);
            else
                posts = postRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else if (status != null && tagId != null) {
            posts = postRepository.findByStatusAndTagsId(status, tagId, pageable);
        } else if (status != null) {
            posts = postRepository.findByStatus(status, pageable);
        } else if (tagId != null) {
            posts = postRepository.findByTagsId(tagId, pageable);
        } else {
            posts = postRepository.findAll(pageable);
        }

        return posts.map(this::toSummaryWithCounts);
    }

    // ===== Authenticated =====

    @Override
    public Page<PostSummaryResponse> getMyPosts(Pageable pageable, PostStatus status) {
        String authorId = GatewayAuthentication.currentUserId();
        Page<Post> posts = status != null
                ? postRepository.findByAuthorIdAndStatus(authorId, status, pageable)
                : postRepository.findByAuthorId(authorId, pageable);
        return posts.map(this::toSummaryWithCounts);
    }

    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        // Bài DRAFT/HIDDEN chỉ owner hoặc ADMIN mới xem được
        if (post.getStatus() != PostStatus.PUBLISHED) {
            String userId = currentUserIdOrNull();
            if (!isAdmin() && !post.getAuthorId().equals(userId))
                throw new BlogException(ErrorCode.ACCESS_DENIED);
        }

        return toResponseWithCounts(post);
    }

    @Override
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        // Bài không PUBLISHED → chỉ owner hoặc ADMIN mới xem được
        // Trả POST_NOT_FOUND để không lộ sự tồn tại của bài draft
        if (post.getStatus() != PostStatus.PUBLISHED) {
            String userId = currentUserIdOrNull();
            if (!isAdmin() && !post.getAuthorId().equals(userId))
                throw new BlogException(ErrorCode.POST_NOT_FOUND);
        }

        // Chỉ tăng view khi PUBLISHED
        if (post.getStatus() == PostStatus.PUBLISHED) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }

        return toResponseWithCounts(post);
    }

    @Override
    public PostResponse createPost(CreatePostRequest request) {
        String authorId = GatewayAuthentication.currentUserId();

        if (postRepository.existsByTitle(request.getTitle()))
            throw new BlogException(ErrorCode.POST_TITLE_EXISTED);

        Post post = postMapper.toPost(request);
        post.setAuthorId(authorId);
        post.setSlug(generateSlug(request.getTitle()));

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            String thumbnailUrl = minioService.uploadFile(request.getThumbnail(), thumbnailFolder);
            post.setThumbnail(thumbnailUrl);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            post.setTags(tags);
        }

        return toResponseWithCounts(postRepository.save(post));
    }

    @Override
    public PostResponse updatePost(Long id, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        checkPostOwner(post);

        if (!post.getTitle().equals(request.getTitle())
                && postRepository.existsByTitle(request.getTitle()))
            throw new BlogException(ErrorCode.POST_TITLE_EXISTED);

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            if (post.getThumbnail() != null) {
                minioService.deleteFile(post.getThumbnail());
            }
            String thumbnailUrl = minioService.uploadFile(request.getThumbnail(), thumbnailFolder);
            post.setThumbnail(thumbnailUrl);
        }

        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            post.setTags(tags);
        } else if (Boolean.TRUE.equals(request.getClearTags())) {
            post.getTags().clear();
        }

        postMapper.updatePost(post, request);
        return toResponseWithCounts(postRepository.save(post));
    }

    @Override
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        checkPostOwner(post);

        if (post.getThumbnail() != null) {
            minioService.deleteFile(post.getThumbnail());
        }

        postRepository.delete(post);
    }

    // ===== Helpers =====

    private void checkPostOwner(Post post) {
        String currentUserId = GatewayAuthentication.currentUserId();
        if (isAdmin()) return;
        if (!post.getAuthorId().equals(currentUserId))
            throw new BlogException(ErrorCode.NOT_POST_OWNER);
    }

    private boolean isAdmin() {
        return "ROLE_ADMIN".equals(GatewayAuthentication.currentRole());
    }

    private String currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof GatewayAuthentication ga) return (String) ga.getPrincipal();
        return null;
    }

    private String generateSlug(String title) {
        // "đ/Đ" không phân rã được qua NFD, cần replace thủ công trước
        String text = title.replace("đ", "d").replace("Đ", "D");

        // NFD tách ký tự có dấu thành base char + combining marks, sau đó xóa marks
        text = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        if (postRepository.existsBySlug(slug))
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);

        return slug;
    }

    private PostResponse toResponseWithCounts(Post post) {
        PostResponse response = postMapper.toPostResponse(post);
        response.setCommentCount(commentRepository.countByPostId(post.getId()));
        response.setUpvoteCount(voteRepository.countByTargetIdAndTargetTypeAndVoteType(
                post.getId(), TargetType.POST, VoteType.UPVOTE));
        response.setDownvoteCount(voteRepository.countByTargetIdAndTargetTypeAndVoteType(
                post.getId(), TargetType.POST, VoteType.DOWNVOTE));
        return response;
    }

    private PostSummaryResponse toSummaryWithCounts(Post post) {
        PostSummaryResponse response = postMapper.toPostSummaryResponse(post);
        response.setCommentCount(commentRepository.countByPostId(post.getId()));
        response.setUpvoteCount(voteRepository.countByTargetIdAndTargetTypeAndVoteType(
                post.getId(), TargetType.POST, VoteType.UPVOTE));
        return response;
    }
}
