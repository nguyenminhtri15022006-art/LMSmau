import { describe, expect, it } from 'vitest';
import { findCourseByRouteSlug, getCourseRouteSlug, slugifyCourseText } from '../../utils/courseSlug';

describe('courseSlug utilities', () => {
  it('slugifies course text with punctuation and spacing', () => {
    expect(slugifyCourseText('  React Basics: 2026!  ')).toBe('react-basics-2026');
    expect(slugifyCourseText(null)).toBe('');
  });

  it('uses slug, title/name slug, then id for route slug', () => {
    expect(getCourseRouteSlug({ slug: 'custom-slug', title: 'React' })).toBe('custom-slug');
    expect(getCourseRouteSlug({ title: 'React Basics' })).toBe('react-basics');
    expect(getCourseRouteSlug({ name: 'Java Core' })).toBe('java-core');
    expect(getCourseRouteSlug({ id: 'course-1' })).toBe('course-1');
  });

  it('finds courses by id, slug, generated title slug, or unique category slug', () => {
    const courses = [
      { id: '1', slug: 'react', title: 'React Basics', categories: [{ slug: 'frontend' }] },
      { id: '2', title: 'Java Core', categories: [{ slug: 'backend' }] },
    ];

    expect(findCourseByRouteSlug(courses, '1')?.id).toBe('1');
    expect(findCourseByRouteSlug(courses, 'react')?.id).toBe('1');
    expect(findCourseByRouteSlug(courses, 'java-core')?.id).toBe('2');
    expect(findCourseByRouteSlug(courses, 'backend')?.id).toBe('2');
  });

  it('returns null for empty, missing, or ambiguous category slugs', () => {
    const courses = [
      { id: '1', categories: [{ slug: 'shared' }] },
      { id: '2', categories: [{ slug: 'shared' }] },
    ];

    expect(findCourseByRouteSlug(courses, '')).toBeNull();
    expect(findCourseByRouteSlug(null, 'shared')).toBeNull();
    expect(findCourseByRouteSlug(courses, 'shared')).toBeNull();
  });
});
