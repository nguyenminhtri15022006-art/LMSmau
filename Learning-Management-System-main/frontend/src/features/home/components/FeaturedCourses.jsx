import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import CourseCard from "../../../components/ui/CourseCard";
import { courseApi } from "../../../services/course.api";
import { identityApi } from "../../../services/identity.api";
import "../styles/FeaturedCourses.css";

const getProfileName = (profile) => profile?.fullname || profile?.fullName || profile?.name;
const getProfileAvatar = (profile) => profile?.avatarUrl || profile?.avatar || profile?.image;

const mapCourseForCard = (c, lessonsCount, instructorProfile) => ({
  id: c.id,
  image: c.thumbnailUrl || c.image || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=600&auto=format&fit=crop',
  title: c.title || 'Untitled Course',
  instructor: c.instructorName || c.instructor || getProfileName(instructorProfile) || 'Giảng viên',
  instructorAvatar: c.instructorAvatar || c.instructorAvatarUrl || getProfileAvatar(instructorProfile) || null,
  level: c.level === 'BEGINNER' ? 'Cơ bản' : c.level === 'INTERMEDIATE' ? 'Trung cấp' : c.level === 'ADVANCED' ? 'Nâng cao' : c.level || 'Cơ bản',
  duration: c.duration ? (typeof c.duration === 'string' && c.duration.includes('tháng') ? c.duration : `${c.duration} tháng`) : null,
  lessons: Number(lessonsCount ?? c.lessons ?? 0),
  exercises: Number(c.exerciseCount ?? c.exercises ?? 0),
});

export default function FeaturedCourses() {
    const [featuredCourses, setFeaturedCourses] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchCourses = async () => {
            setLoading(true);
            try {
                let allCourses = [];
                try {
                    const res = await courseApi.getAll({ status: 'PUBLIC', size: 1000 });
                    allCourses = res?.data?.data?.content || res?.data?.content || [];
                } catch (apiErr) {
                    console.warn("Backend courseApi.getAll failed:", apiErr);
                }

                if (allCourses && allCourses.length > 0) {
                    // Fetch student counts for all courses in parallel to sort them
                    const studentCounts = await Promise.all(
                        allCourses.map(c => courseApi.getStudentsCount(c.id).catch(() => 0))
                    );

                    // Add student counts to course objects
                    const coursesWithStudents = allCourses.map((c, i) => ({
                        ...c,
                        studentsCount: studentCounts[i]
                    }));

                    // Sort by student count descending, and select the top 4 courses
                    const top4Courses = coursesWithStudents
                        .sort((a, b) => b.studentsCount - a.studentsCount)
                        .slice(0, 4);

                    // Fetch lessons count only for the top 4 featured courses in parallel
                    const lessonsCounts = await Promise.all(
                        top4Courses.map(c => courseApi.getLessonsCount(c.id).catch(() => 0))
                    );

                    const instructorProfiles = await Promise.all(
                        top4Courses.map((c) => (
                            c.instructorId
                                ? identityApi.getPublicProfile(c.instructorId).catch(() => null)
                                : Promise.resolve(null)
                        ))
                    );

                    setFeaturedCourses(top4Courses.map((c, i) => mapCourseForCard(c, lessonsCounts[i], instructorProfiles[i])));
                }
            } catch (err) {
                console.error("Failed to load featured courses by student count:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchCourses();
    }, []);

    // IntersectionObserver fix for asynchronously loaded data-reveal elements
    useEffect(() => {
        if (!loading && featuredCourses.length > 0) {
            const elements = document.querySelectorAll("#courses [data-reveal]");
            const observer = new IntersectionObserver(
                (entries) => {
                    entries.forEach((entry) => {
                        if (entry.isIntersecting) {
                            entry.target.classList.add("is-visible");
                            observer.unobserve(entry.target);
                        }
                    });
                },
                { threshold: 0.1, rootMargin: "0px 0px -40px 0px" }
            );
            elements.forEach((el) => observer.observe(el));
            return () => observer.disconnect();
        }
    }, [loading, featuredCourses]);

    return (
        <section id="courses" className="home-courses">
            <div className="home-courses__inner">
                <div className="home-courses__header" data-reveal>
                    <div>
                        <h2 className="home-courses__title">Khóa học nổi bật</h2>
                        <p className="home-courses__sub">
                            Những khóa học chất lượng có số lượng học viên tham gia đông đảo nhất hệ thống EduLearn.
                        </p>
                    </div>
                    <Link to="/list-course" className="home-courses__view-all">
                        Xem tất cả
                        <span className="material-symbols-outlined" style={{ fontSize: "16px", marginLeft: "4px" }}>arrow_forward</span>
                    </Link>
                </div>

                {loading ? (
                    <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
                        Đang tải danh sách khóa học nổi bật...
                    </div>
                ) : featuredCourses.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
                        Chưa có khóa học nổi bật nào.
                    </div>
                ) : (
                    <div className="home-courses__grid">
                        {featuredCourses.map((course, index) => (
                            <div data-reveal style={{ "--reveal-delay": `${index * 70}ms` }} key={course.id}>
                                <CourseCard course={course} />
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </section>
    );
}
