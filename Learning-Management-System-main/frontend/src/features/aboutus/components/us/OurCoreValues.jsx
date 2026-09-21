import "../../styles/us/OurCoreValue.css";

const values = [
    {
        icon: "terminal",
        title: "Thực hành trước",
        text: "Học viên được kiểm tra kiến thức bằng quiz và coding challenge.",
    },
    {
        icon: "timeline",
        title: "Rõ ràng tiến độ",
        text: "Hệ thống lưu khóa học đã đăng ký, tiến trình học và kết quả làm bài.",
    },
    {
        icon: "admin_panel_settings",
        title: "Tách biệt vai trò",
        text: "Student, Instructor và Admin có giao diện, quyền truy cập riêng.",
    },
    {
        icon: "co_present",
        title: "Hỗ trợ giảng dạy",
        text: "Giảng viên có công cụ tạo khóa học, bài kiểm tra và theo dõi học viên.",
    },
    {
        icon: "hub",
        title: "Mở rộng được",
        text: "Kiến trúc microservices giúp từng service phát triển độc lập.",
    },
    {
        icon: "forum",
        title: "Cộng đồng học tập",
        text: "Blog, bình luận và thảo luận giúp người học chia sẻ kiến thức.",
    },
];

export default function OurCoreValue() {
    return (
        <section className="core-value">
            <div className="core-value__inner">
                <div className="core-value__top" data-reveal>
                    <h2 className="core-value__title">Giá trị cốt lõi</h2>
                    <p className="core-value__desc">
                        Những nguyên tắc định hướng EduLearn trong việc xây dựng một nền tảng học tập
                        có thực hành, có tiến độ và có cộng đồng.
                    </p>
                </div>
                <div className="core-value__bottom">
                    {values.map((value, index) => (
                        <div
                            className="core-value__item"
                            key={value.title}
                            data-reveal
                            style={{ "--reveal-delay": `${index * 45}ms` }}
                        >
                            <div className="core-value__logo">
                                <span className="material-symbols-outlined">{value.icon}</span>
                            </div>
                            <h3 className="core-value__title__item">{value.title}</h3>
                            <p className="core-value__desc">{value.text}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
