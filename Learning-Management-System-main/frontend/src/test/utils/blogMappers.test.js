import { describe, it, expect } from 'vitest';
import {
  mapBlogPost,
  mapBlogComment,
  unwrapApiData,
  unwrapPageContent,
  mapBlogTagToCategory,
  ALL_BLOG_CATEGORY,
} from '../../utils/blogMappers';

describe('unwrapApiData', () => {
  it('unwraps nested ApiResponse format', () => {
    const response = { data: { data: { id: '1' }, message: 'OK' } };
    expect(unwrapApiData(response)).toEqual({ id: '1' });
  });

  it('falls back to response.data', () => {
    const response = { data: { id: '1' } };
    expect(unwrapApiData(response)).toEqual({ id: '1' });
  });

  it('falls back to response itself', () => {
    const response = { id: '1' };
    expect(unwrapApiData(response)).toEqual({ id: '1' });
  });
});

describe('unwrapPageContent', () => {
  it('extracts content array from page', () => {
    const response = { data: { data: { content: [{ id: '1' }, { id: '2' }] } } };
    expect(unwrapPageContent(response)).toHaveLength(2);
  });

  it('returns raw array if payload is directly an array', () => {
    const response = { data: { data: [{ id: '1' }] } };
    expect(unwrapPageContent(response)).toHaveLength(1);
  });
});

describe('mapBlogTagToCategory', () => {
  it('maps tag to category shape', () => {
    expect(mapBlogTagToCategory({ id: 't-1', name: 'React' }))
      .toEqual({ id: 't-1', name: 'React' });
  });
});

describe('mapBlogPost', () => {
  const post = {
    id: 'post-1',
    title: 'Learning React',
    summary: 'React is a library...',
    content: '<p>React content</p>',
    authorName: 'John Doe',
    authorAvatar: 'https://example.com/avatar.jpg',
    createdAt: '2024-06-15T10:00:00Z',
    thumbnail: 'https://example.com/thumb.jpg',
    tags: [{ id: 't-1', name: 'React' }],
    upvoteCount: 10,
    downvoteCount: 2,
    userVote: 'upvote',
  };

  it('maps post with all derived fields', () => {
    const result = mapBlogPost(post);
    expect(result.id).toBe('post-1');
    expect(result.title).toBe('Learning React');
    expect(result.author).toBe('John Doe');
    expect(result.category).toBe('React');
    expect(result.readTime).toContain('phút đọc');
    expect(result.userVote).toBe('upvote');
    expect(result.image).toBe('https://example.com/thumb.jpg');
  });

  it('strips HTML from excerpt', () => {
    const htmlPost = { ...post, summary: '', content: '<h1>Hello</h1><p>World</p>' };
    const result = mapBlogPost(htmlPost);
    expect(result.excerpt).not.toContain('<');
    expect(result.excerpt).toContain('Hello World');
  });

  it('uses fallback image when thumbnail is missing', () => {
    const noThumb = { ...post, thumbnail: null };
    expect(mapBlogPost(noThumb).image).toContain('images.unsplash.com');
  });

  it('maps all userVote variants', () => {
    // userVote takes priority
    expect(mapBlogPost({ ...post, userVote: 'upvote', myVote: 'downvote' }).userVote).toBe('upvote');
    // myVote used when userVote is missing
    expect(mapBlogPost({ id: 'p-1', title: 'T', createdAt: '2024-06-15T10:00:00Z', myVote: 'downvote' }).userVote).toBe('downvote');
    // voteType used when both userVote and myVote are missing
    expect(mapBlogPost({ id: 'p-1', title: 'T', createdAt: '2024-06-15T10:00:00Z', voteType: null }).userVote).toBeNull();
  });

  it('formats date in Vietnamese locale', () => {
    const result = mapBlogPost(post);
    expect(result.date).toContain('/');
  });
});

describe('mapBlogComment', () => {
  const comment = {
    id: 'c-1',
    content: 'Great post!',
    authorName: 'Jane',
    authorAvatar: 'https://example.com/jane.jpg',
    authorRole: 'STUDENT',
    createdAt: '2024-06-15T11:00:00Z',
    upvoteCount: 5,
    replies: [
      {
        id: 'c-2',
        content: 'Thanks!',
        authorName: 'John',
        authorRole: 'INSTRUCTOR',
        createdAt: '2024-06-15T12:00:00Z',
        upvoteCount: 3,
      },
    ],
  };

  it('maps comment with nested replies', () => {
    const result = mapBlogComment(comment);
    expect(result.author).toBe('Jane');
    expect(result.role).toBe('STUDENT');
    expect(result.replies).toHaveLength(1);
    expect(result.replies[0].author).toBe('John');
  });

  it('falls back author name for UUID-like authors', () => {
    const uuidAuthor = { ...comment, authorName: '550e8400-e29b-41d4-a716-446655440000' };
    expect(mapBlogComment(uuidAuthor).author).toBe('Người dùng');
  });
});
