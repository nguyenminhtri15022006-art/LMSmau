export const COURSE_DISPLAY_SORT = "updatedAt,desc";

function getCourseSortTimestamp(course) {
    const rawValue = course?.updatedAt ?? course?.createdAt ?? null;
    if (!rawValue) return 0;

    const timestamp = new Date(rawValue).getTime();
    return Number.isNaN(timestamp) ? 0 : timestamp;
}

export function sortCoursesByDisplayOrder(courses = []) {
    return [...courses].sort((a, b) => {
        const timeDiff = getCourseSortTimestamp(b) - getCourseSortTimestamp(a);
        if (timeDiff !== 0) return timeDiff;

        const titleA = (a?.title ?? a?.name ?? "").toLowerCase();
        const titleB = (b?.title ?? b?.name ?? "").toLowerCase();
        return titleA.localeCompare(titleB, "vi");
    });
}
