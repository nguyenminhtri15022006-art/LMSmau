import React, { useState, useEffect } from "react";
import { Navigate, useLocation } from "react-router-dom";
import AnimatedPage from "../../../../components/ui/AnimatedPage";
import MyCoursesContent from "../../components/student/MyCourse/MyCourseContent";
import { courseApi } from "../../../../services/course.api";
import { identityApi } from "../../../../services/identity.api";
import { buildAppErrorState, getAppErrorRoute } from "../../../../utils/appError";

const LEVEL_MAP = {
    BEGINNER: "Cơ bản",
    INTERMEDIATE: "Trung cấp",
    ADVANCED: "Nâng cao",
};

const FALLBACK_IMAGE = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=2070&auto=format&fit=crop";

export default function MyCoursesPage() {
    const location = useLocation();
    const [activeTab, setActiveTab] = useState("all");
    const [courses, setCourses] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setIsLoading(true);
                const enrolledList = await courseApi.getEnrolled();

                const withProgress = await Promise.all(
                    enrolledList.map(async (course) => {
                        try {
                            const progress = await courseApi.getProgress(course.id);
                            return { course, progress };
                        } catch {
                            return { course, progress: null };
                        }
                    })
                );

                const uniqueInstructorIds = [...new Set(enrolledList.map((course) => course.instructorId).filter(Boolean))];
                console.log("[MyCourse] instructorIds to fetch:", uniqueInstructorIds);
                const instructorProfiles = await Promise.all(
                    uniqueInstructorIds.map((id) => identityApi.getPublicProfile(id).catch((err) => {
                        console.warn("[MyCourse] getPublicProfile failed for", id, err?.response?.status);
                        return null;
                    }))
                );
                const instructorMap = {};
                uniqueInstructorIds.forEach((id, index) => {
                    instructorMap[id] = instructorProfiles[index]?.fullname || "Giảng viên";
                });

                const mapped = withProgress.map(({ course, progress }) => {
                    const progressPercent = progress?.percentComplete ?? 0;
                    return {
                        id: course.id,
                        title: course.title,
                        description: course.description || "",
                        instructor: instructorMap[course.instructorId] || "Giảng viên",
                        image: course.thumbnailUrl || FALLBACK_IMAGE,
                        badge: course.categories?.[0]?.name || "",
                        level: LEVEL_MAP[course.level] || course.level || "Cơ bản",
                        lessons: progress?.totalLessons ?? 0,
                        exercises: course.exerciseCount ?? 0,
                        rating: 0,
                        students: 0,
                        progress: progressPercent,
                        status: progressPercent >= 100 ? "Completed" : "In Progress",
                    };
                });

                setCourses(mapped);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch enrolled courses:", err);
                setError(buildAppErrorState(err, {
                    title: "Không thể tải danh sách khóa học của tôi",
                    fallbackPath: "/dashboard",
                }));
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, []);

    if (error) {
        return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
    }

    return (
        <AnimatedPage>
            <MyCoursesContent
                courses={courses}
                activeTab={activeTab}
                onTabChange={setActiveTab}
                isLoading={isLoading}
                error={null}
            />
        </AnimatedPage>
    );
}
