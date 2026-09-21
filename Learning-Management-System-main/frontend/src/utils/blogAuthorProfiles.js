import identityApi from '../services/identity.api';

const getProfileName = (profile) => profile?.fullname || profile?.fullName || profile?.name;
const getProfileAvatar = (profile) => profile?.avatarUrl || profile?.avatar || profile?.image;
const getProfileId = (profile) => profile?.userId || profile?.id || profile?.sub;

const hasAccessToken = () => Boolean(
  localStorage.getItem('access_token') || sessionStorage.getItem('access_token'),
);

const fetchPublicProfile = async (userId) => {
  try {
    return await identityApi.getPublicProfile(userId);
  } catch (_publicErr) {
    if (!hasAccessToken()) return null;

    try {
      return await identityApi.getPublicProfile(userId, { skipAuth: false });
    } catch (_authErr) {
      return null;
    }
  }
};

const resolveProfile = (profilesById, user, ownerId) => {
  const profile = profilesById[ownerId];
  if (profile) return profile;

  return ownerId && ownerId === getProfileId(user) ? user : null;
};

export const attachBlogAuthorProfiles = async (posts = [], currentUser = null) => {
  const authorIds = [...new Set(posts.map(post => post?.authorId).filter(Boolean))];

  if (authorIds.length === 0) {
    return posts;
  }

  const profileEntries = await Promise.all(
    authorIds.map(async (authorId) => {
      return [authorId, await fetchPublicProfile(authorId)];
    }),
  );

  const profilesByAuthorId = Object.fromEntries(profileEntries);

  return posts.map((post) => {
    const profile = resolveProfile(profilesByAuthorId, currentUser, post.authorId);
    if (!profile) return post;

    return {
      ...post,
      authorName: getProfileName(profile) || post.authorName,
      authorAvatar: getProfileAvatar(profile) || post.authorAvatar,
      authorBio: profile.bio || post.authorBio,
      authorRole: profile.role || post.authorRole,
    };
  });
};

const collectCommentUserIds = (comments = []) => comments.flatMap(comment => [
  comment?.userId,
  ...collectCommentUserIds(comment?.replies || []),
]).filter(Boolean);

const mapCommentWithProfiles = (comment, profilesByUserId) => {
  const profile = resolveProfile(profilesByUserId.profiles, profilesByUserId.currentUser, comment.userId);

  return {
    ...comment,
    authorName: getProfileName(profile) || comment.authorName,
    authorAvatar: getProfileAvatar(profile) || comment.authorAvatar,
    authorRole: profile?.role || comment.authorRole,
    replies: (comment.replies || []).map(reply => mapCommentWithProfiles(reply, profilesByUserId)),
  };
};

export const attachBlogCommentProfiles = async (comments = [], currentUser = null) => {
  const userIds = [...new Set(collectCommentUserIds(comments))];

  if (userIds.length === 0) {
    return comments;
  }

  const profileEntries = await Promise.all(
    userIds.map(async (userId) => {
      return [userId, await fetchPublicProfile(userId)];
    }),
  );

  const profilesByUserId = {
    profiles: Object.fromEntries(profileEntries),
    currentUser,
  };

  return comments.map(comment => mapCommentWithProfiles(comment, profilesByUserId));
};
