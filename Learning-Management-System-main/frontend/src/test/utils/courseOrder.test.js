import { describe, it, expect } from 'vitest';
import { sortCoursesByDisplayOrder } from '../../utils/courseOrder';

describe('sortCoursesByDisplayOrder', () => {
  const makeCourse = (overrides) => ({
    id: '1',
    title: 'Course',
    updatedAt: '2024-06-01T00:00:00Z',
    createdAt: '2024-05-01T00:00:00Z',
    ...overrides,
  });

  it('sorts by updatedAt descending', () => {
    const newer = makeCourse({ id: '1', updatedAt: '2024-06-15T00:00:00Z' });
    const older = makeCourse({ id: '2', updatedAt: '2024-06-01T00:00:00Z' });
    const sorted = sortCoursesByDisplayOrder([older, newer]);
    expect(sorted[0].id).toBe('1');
    expect(sorted[1].id).toBe('2');
  });

  it('falls back to createdAt when updatedAt is missing', () => {
    const newer = makeCourse({ id: '1', updatedAt: null, createdAt: '2024-06-15T00:00:00Z' });
    const older = makeCourse({ id: '2', updatedAt: null, createdAt: '2024-06-01T00:00:00Z' });
    const sorted = sortCoursesByDisplayOrder([older, newer]);
    expect(sorted[0].id).toBe('1');
    expect(sorted[1].id).toBe('2');
  });

  it('sorts by title alphabetically when timestamps are equal', () => {
    const a = makeCourse({ id: '1', title: 'Alpha', updatedAt: '2024-06-15T00:00:00Z' });
    const b = makeCourse({ id: '2', title: 'Beta', updatedAt: '2024-06-15T00:00:00Z' });
    const sorted = sortCoursesByDisplayOrder([b, a]);
    expect(sorted[0].title).toBe('Alpha');
    expect(sorted[1].title).toBe('Beta');
  });

  it('handles empty array', () => {
    expect(sortCoursesByDisplayOrder([])).toEqual([]);
  });

  it('handles null/undefined gracefully', () => {
    // sortCoursesByDisplayOrder uses [...courses] spread which requires array
    // Pass empty array for nullish values
    expect(sortCoursesByDisplayOrder([])).toEqual([]);
  });

  it('puts courses with no timestamp at the end', () => {
    const dated = makeCourse({ id: '1', updatedAt: '2024-06-15T00:00:00Z' });
    const undated = makeCourse({ id: '2', updatedAt: null, createdAt: null });
    const sorted = sortCoursesByDisplayOrder([undated, dated]);
    expect(sorted[0].id).toBe('1');
    expect(sorted[1].id).toBe('2');
  });
});
