import { formatDateVN } from './dateTime';

const defaultAvatar =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239CA3AF'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88C7.55 15.8 9.68 15 12 15s4.45.8 6.14 2.12C16.43 19.18 14.03 20 12 20z'/%3E%3C/svg%3E";

const getAvatarOrDefault = (avatarUrl) => {
  if (
    !avatarUrl ||
    typeof avatarUrl !== 'string' ||
    avatarUrl === 'default' ||
    avatarUrl.includes('anhnhom.png')
  ) {
    return defaultAvatar;
  }
  return avatarUrl;
};

export const ALL_BLOG_CATEGORY = 'Tất cả bài viết';

const FALLBACK_POST_IMAGE =
  'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80';

const stripHtml = (value = '') => value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();

const formatDate = (value) => {
  if (!value) return '';

  return formatDateVN(value) || value;
};

const estimateReadTime = (content = '', summary = '') => {
  const text = stripHtml(content || summary);
  const words = text ? text.split(/\s+/).length : 0;
  return `${Math.max(1, Math.ceil(words / 220))} phút đọc`;
};

const getCategory = (tags = []) => tags[0]?.name || 'Chưa phân loại';

const getPostDetailParam = (post) => post.detailParam || post.slug || post.id;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const isUuid = (value) => typeof value === 'string' && UUID_PATTERN.test(value);
const displayNameOrFallback = (value, fallback) => (value && !isUuid(value) ? value : fallback);

export const unwrapApiData = (response) => response?.data?.data ?? response?.data ?? response;

export const unwrapPageContent = (response) => {
  const payload = unwrapApiData(response);
  return Array.isArray(payload) ? payload : payload?.content ?? [];
};

export const mapBlogTagToCategory = (tag) => ({
  id: tag.id,
  name: tag.name,
});

export const mapBlogPost = (post) => {
  const category = getCategory(post.tags);
  const userVote = post.userVote || post.myVote || post.voteType || post.currentUserVote || null;

  return {
    ...post,
    id: post.id,
    detailParam: getPostDetailParam(post),
    title: post.title,
    excerpt: post.summary || post.excerpt || stripHtml(post.content).slice(0, 180),
    author: displayNameOrFallback(post.authorName || post.author, 'Tác giả'),
    authorAvatar: getAvatarOrDefault(post.authorAvatar),
    date: formatDate(post.createdAt) || post.date,
    readTime: post.readTime || estimateReadTime(post.content, post.summary || post.excerpt),
    category: post.category || category,
    image: post.thumbnail || post.image || FALLBACK_POST_IMAGE,
    content: post.content,
    tags: post.tags || [],
    upvoteCount: post.upvoteCount ?? post.upvotes ?? post.likeCount ?? 0,
    downvoteCount: post.downvoteCount ?? post.downvotes ?? post.dislikeCount ?? 0,
    userVote: typeof userVote === 'string' ? userVote.toLowerCase() : userVote,
  };
};

export const mapBlogComment = (comment) => {
  const userVote = comment.userVote || comment.myVote || comment.voteType || comment.currentUserVote || null;
  return {
    ...comment,
    author: displayNameOrFallback(comment.authorName || comment.author, 'Người dùng'),
    authorAvatar: getAvatarOrDefault(comment.authorAvatar),
    role: comment.authorRole,
    time: formatDate(comment.createdAt),
    upvoteCount: comment.upvoteCount ?? 0,
    downvoteCount: comment.downvoteCount ?? 0,
    userVote: typeof userVote === 'string' ? userVote.toLowerCase() : null,
    replies: (comment.replies || []).map(mapBlogComment),
  };
};
