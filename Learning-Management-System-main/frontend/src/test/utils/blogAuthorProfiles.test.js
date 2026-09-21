import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../services/identity.api', () => ({
  default: {
    getPublicProfile: vi.fn(),
  },
}));

import identityApi from '../../services/identity.api';
import { attachBlogAuthorProfiles, attachBlogCommentProfiles } from '../../utils/blogAuthorProfiles';

describe('blogAuthorProfiles utilities', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('returns original posts when there are no author ids', async () => {
    const posts = [{ id: 1, title: 'Post' }];
    await expect(attachBlogAuthorProfiles(posts)).resolves.toBe(posts);
    expect(identityApi.getPublicProfile).not.toHaveBeenCalled();
  });

  it('attaches public author profile fields to posts', async () => {
    identityApi.getPublicProfile.mockResolvedValueOnce({
      fullname: 'Jane Doe',
      avatarUrl: 'avatar.png',
      bio: 'Teacher',
      role: 'INSTRUCTOR',
    });

    await expect(attachBlogAuthorProfiles([{ id: 1, authorId: 'u-1', authorName: 'Old' }])).resolves.toEqual([
      {
        id: 1,
        authorId: 'u-1',
        authorName: 'Jane Doe',
        authorAvatar: 'avatar.png',
        authorBio: 'Teacher',
        authorRole: 'INSTRUCTOR',
      },
    ]);
  });

  it('falls back to authenticated profile lookup when public profile fails and token exists', async () => {
    localStorage.setItem('access_token', 'token');
    identityApi.getPublicProfile
      .mockRejectedValueOnce(new Error('public failed'))
      .mockResolvedValueOnce({ fullName: 'Private User' });

    const result = await attachBlogAuthorProfiles([{ id: 1, authorId: 'u-1', authorName: 'Old' }]);

    expect(identityApi.getPublicProfile).toHaveBeenNthCalledWith(2, 'u-1', { skipAuth: false });
    expect(result[0].authorName).toBe('Private User');
  });

  it('uses current user profile when remote profile is unavailable', async () => {
    identityApi.getPublicProfile.mockResolvedValueOnce(null);

    const result = await attachBlogAuthorProfiles(
      [{ id: 1, authorId: 'u-1', authorName: 'Old' }],
      { id: 'u-1', name: 'Current User', image: 'me.png' },
    );

    expect(result[0].authorName).toBe('Current User');
    expect(result[0].authorAvatar).toBe('me.png');
  });

  it('attaches profiles to nested comments and replies', async () => {
    identityApi.getPublicProfile
      .mockResolvedValueOnce({ userId: 'u-1', name: 'Root User', avatar: 'root.png' })
      .mockResolvedValueOnce({ userId: 'u-2', name: 'Reply User', role: 'STUDENT' });

    const result = await attachBlogCommentProfiles([
      {
        id: 'c-1',
        userId: 'u-1',
        authorName: 'Old Root',
        replies: [{ id: 'r-1', userId: 'u-2', authorName: 'Old Reply' }],
      },
    ]);

    expect(result[0].authorName).toBe('Root User');
    expect(result[0].authorAvatar).toBe('root.png');
    expect(result[0].replies[0].authorName).toBe('Reply User');
    expect(result[0].replies[0].authorRole).toBe('STUDENT');
  });
});
