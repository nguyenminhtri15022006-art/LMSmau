export function slugifyCourseText(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function getCourseRouteSlug(course) {
  return course?.slug || slugifyCourseText(course?.title || course?.name) || course?.id;
}

export function findCourseByRouteSlug(courses, routeSlug) {
  const list = Array.isArray(courses) ? courses : [];
  const normalizedRouteSlug = String(routeSlug || '');
  if (!normalizedRouteSlug) return null;

  const directMatch = list.find((course) =>
    String(course?.id || '') === normalizedRouteSlug ||
    String(course?.slug || '') === normalizedRouteSlug ||
    slugifyCourseText(course?.title || course?.name) === normalizedRouteSlug
  );

  if (directMatch) return directMatch;

  const categoryMatches = list.filter((course) =>
    String(course?.categories?.[0]?.slug || '') === normalizedRouteSlug
  );

  return categoryMatches.length === 1 ? categoryMatches[0] : null;
}
