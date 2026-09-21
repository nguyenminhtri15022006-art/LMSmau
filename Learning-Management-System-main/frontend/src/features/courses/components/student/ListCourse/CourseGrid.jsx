import CourseCard from "../../../../../components/ui/CourseCard"
import "../../../styles/student/ListCourse/CourseGrid.css";

export default function CourseGrid({ courses }) {
    return (
        <div className="course-grid">
            {courses.map((course, index) => (
                <CourseCard key={course.id} course={course} index={index} />
            ))}
        </div>
    )
}
