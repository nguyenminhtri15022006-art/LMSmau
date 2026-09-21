import AnimatedPage from "../../../../components/ui/AnimatedPage";
import ListCourseContent from "../../components/student/ListCourse/ListCourseContent";

export default function ListCoursePage() {
    return (
        <AnimatedPage>
            <div className="list-course-page-shell">
                <ListCourseContent />
            </div>
        </AnimatedPage>
    );
}
