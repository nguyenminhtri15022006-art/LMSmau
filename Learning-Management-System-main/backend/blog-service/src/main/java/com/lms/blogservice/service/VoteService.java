package com.lms.blogservice.service;

import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.dto.response.VoteResponse;

public interface VoteService {
    VoteResponse votePost(Long postId, VoteRequest request);
    VoteResponse voteComment(Long postId, Long commentId, VoteRequest request);
}
