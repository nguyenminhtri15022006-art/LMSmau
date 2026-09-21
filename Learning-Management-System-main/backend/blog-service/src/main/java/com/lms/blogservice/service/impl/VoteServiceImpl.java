package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.dto.response.VoteResponse;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Vote;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.VoteService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteServiceImpl implements VoteService {

    PostRepository postRepository;
    CommentRepository commentRepository;
    VoteRepository voteRepository;

    @Override
    @Transactional
    public VoteResponse votePost(Long postId, VoteRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        if(post.getAuthorId().equals(userId))
            throw new BlogException(ErrorCode.CANNOT_VOTE_OWN_POST);

        if(!post.getStatus().equals(PostStatus.PUBLISHED))
            throw new BlogException(ErrorCode.POST_NOT_PUBLISHED);

        VoteType result = processVote(userId, postId, TargetType.POST, request.getVoteType());
        return buildVoteResponse(postId, TargetType.POST, result);
    }

    @Override
    @Transactional
    public VoteResponse voteComment(Long postId, Long commentId, VoteRequest request) {
        String userId = GatewayAuthentication.currentUserId();

        // Kiểm tra post tồn tại
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BlogException(
                        ErrorCode.COMMENT_NOT_FOUND));

        if(!post.getStatus().equals(PostStatus.PUBLISHED))
            throw new BlogException(ErrorCode.POST_NOT_PUBLISHED);

        // Kiểm tra comment thuộc post này
        if (!comment.getPost().getId().equals(postId))
            throw new BlogException(ErrorCode.COMMENT_NOT_BELONG_TO_POST);


        // Không vote comment của chính mình
        if (comment.getUserId().equals(userId))
            throw new BlogException(ErrorCode.CANNOT_VOTE_OWN_COMMENT);
        VoteType result = processVote(userId, commentId, TargetType.COMMENT, request.getVoteType());
        return buildVoteResponse(commentId, TargetType.COMMENT, result);
    }

    //=============== HELPER ================
    VoteType processVote(String userId, Long targetId,
                                     TargetType targetType, VoteType voteType) {
        Optional<Vote> existingVote = voteRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getVoteType().equals(voteType)) {
                voteRepository.delete(vote);
                return null;
            } else {
                vote.setVoteType(voteType);
                voteRepository.save(vote);
                return voteType;
            }
        } else {
            voteRepository.save(Vote.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .voteType(voteType)
                    .userId(userId)
                    .build()
            );
            return voteType;
        }
    }

    private VoteResponse buildVoteResponse(Long targetId, TargetType targetType, VoteType currentVoteType) {
        return VoteResponse.builder()
                .targetId(targetId)
                .targetType(targetType)
                .voteType(currentVoteType)
                .upvoteCount(voteRepository.countByTargetIdAndTargetTypeAndVoteType(targetId, targetType, VoteType.UPVOTE))
                .downvoteCount(voteRepository.countByTargetIdAndTargetTypeAndVoteType(targetId, targetType, VoteType.DOWNVOTE))
                .build();
    }
}
