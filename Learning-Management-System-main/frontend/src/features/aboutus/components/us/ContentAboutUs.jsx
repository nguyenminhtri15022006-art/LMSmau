import "../../styles/us/ContentAboutUs.css";

export default function ContentAboutUs() {
    return (
        <section id="about-system" className="content">
            <div className="content__inner" data-reveal>
                <div className="content__text">
                    <span className="content__eyebrow">Câu chuyện dự án</span>
                    <h2 className="content__title">Một hệ thống học tập không tách rời thực hành</h2>
                    <div className="content__copy">
                        <p className="content__desc">
                            EduLearn ra đời từ nhu cầu xây dựng một hệ thống quản lý học tập hiện đại,
                            nơi người học không chỉ xem bài giảng mà còn có thể thực hành, kiểm tra kiến thức
                            và theo dõi quá trình học.
                        </p>
                        <p className="content__desc">
                            Thay vì tách rời khóa học, bài tập, blog và quản lý người dùng thành nhiều
                            công cụ khác nhau, dự án gom các chức năng cốt lõi vào một nền tảng thống nhất
                            cho học viên và giảng viên.
                        </p>
                    </div>
                </div>
            </div>
        </section>
    );
}
