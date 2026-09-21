CREATE INDEX idx_posts_author_id ON posts (author_id);
CREATE INDEX idx_posts_status ON posts (status);
CREATE INDEX idx_posts_author_status ON posts (author_id, status);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX idx_posts_view_count ON posts (view_count DESC);
CREATE INDEX idx_posts_title ON posts (title);

CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);
CREATE INDEX idx_comments_user_id ON comments (user_id);
CREATE INDEX idx_comments_created_at ON comments (created_at);

CREATE INDEX idx_votes_target ON votes (target_id, target_type);
CREATE INDEX idx_votes_target_type ON votes (target_id, target_type, vote_type);
CREATE INDEX idx_votes_user_target ON votes (user_id, target_id, target_type);

CREATE INDEX idx_pt_post_id ON post_tags (post_id);
CREATE INDEX idx_pt_tag_id ON post_tags (tag_id);

CREATE INDEX idx_tags_slug ON tags (slug);
