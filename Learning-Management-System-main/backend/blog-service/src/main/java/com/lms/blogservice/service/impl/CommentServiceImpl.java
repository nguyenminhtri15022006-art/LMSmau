package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.UpdateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.CommentMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.CommentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {

    PostRepository postRepository;
    CommentRepository commentRepository;
    VoteRepository voteRepository;
    CommentMapper commentMapper;

    @Override
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        // Kiểm tra post tồn tại và đã published
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED)
            throw new BlogException(ErrorCode.POST_NOT_PUBLISHED);

        // Lấy các comment gốc (không có parent)
        return commentRepository
                .findByPostIdAndParentIsNull(postId, pageable)
                .map(this::toResponseWithRepliesAndCounts);
    }

    @Override
    public CommentResponse createComment(Long postId,
                                         CreateCommentRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED)
            throw new BlogException(ErrorCode.POST_NOT_PUBLISHED);

        Comment comment = Comment.builder()
                .post(post)
                .userId(userId)
                .content(request.getContent())
                .build();

        // Xử lý reply
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BlogException(
                            ErrorCode.COMMENT_NOT_FOUND));

            // Kiểm tra parent có thuộc post này không
            if (!parent.getPost().getId().equals(postId))
                throw new BlogException(ErrorCode.COMMENT_NOT_BELONG_TO_POST);

            // Không cho reply của reply (chỉ 2 cấp)
            if (parent.getParent() != null)
                throw new BlogException(ErrorCode.CANNOT_REPLY_TO_REPLY);

            comment.setParent(parent);
        }

        return toResponseWithRepliesAndCounts(commentRepository.save(comment));
    }

    @Override
    public CommentResponse updateComment(Long postId, Long commentId,
                                         UpdateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED)
            throw new BlogException(ErrorCode.POST_NOT_PUBLISHED);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BlogException(
                        ErrorCode.COMMENT_NOT_FOUND));

        // Kiểm tra comment thuộc post này
        if (!comment.getPost().getId().equals(postId))
            throw new BlogException(ErrorCode.COMMENT_NOT_BELONG_TO_POST);

        // Kiểm tra quyền sở hữu
        checkCommentOwner(comment);

        commentMapper.updateComment(comment, request);
        return toResponseWithRepliesAndCounts(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(Long postId, Long commentId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BlogException(
                        ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId))
            throw new BlogException(ErrorCode.COMMENT_NOT_BELONG_TO_POST);

        checkCommentOwner(comment);

        // Xóa replies trước để tránh FK constraint của Hibernate (không có CASCADE)
        List<Comment> replies = commentRepository.findByParentId(commentId);
        if (!replies.isEmpty()) commentRepository.deleteAll(replies);

        commentRepository.delete(comment);
    }

    // ===== Helper =====
    private void checkCommentOwner(Comment comment) {
        String currentUserId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();
        if ("ROLE_ADMIN".equals(currentRole)) return;
        if (!comment.getUserId().equals(currentUserId))
            throw new BlogException(ErrorCode.NOT_COMMENT_OWNER);
    }

    private CommentResponse toResponseWithRepliesAndCounts(Comment comment) {
        List<Comment> replies = comment.getParent() == null
                ? commentRepository.findByParentId(comment.getId())
                : List.of();

        // Batch: 1 query upvote + 1 query downvote cho toàn bộ comment + replies
        List<Long> allIds = Stream.concat(
                Stream.of(comment.getId()),
                replies.stream().map(Comment::getId)
        ).toList();

        Map<Long, Long> upvotes   = batchVoteCounts(allIds, VoteType.UPVOTE);
        Map<Long, Long> downvotes = batchVoteCounts(allIds, VoteType.DOWNVOTE);

        CommentResponse response = commentMapper.toCommentResponse(comment);
        response.setUpvoteCount(upvotes.getOrDefault(comment.getId(), 0L));
        response.setDownvoteCount(downvotes.getOrDefault(comment.getId(), 0L));

        if (!replies.isEmpty()) {
            response.setReplies(replies.stream().map(reply -> {
                CommentResponse r = commentMapper.toCommentResponse(reply);
                r.setUpvoteCount(upvotes.getOrDefault(reply.getId(), 0L));
                r.setDownvoteCount(downvotes.getOrDefault(reply.getId(), 0L));
                return r;
            }).toList());
        }

        return response;
    }

    private Map<Long, Long> batchVoteCounts(List<Long> ids, VoteType voteType) {
        return voteRepository
                .countByTargetIdsAndTypeAndVoteType(ids, TargetType.COMMENT, voteType)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
