import "../../styles/us/MissionAndVision.css";

export default function MissionAndVision() {
    return (
        <section className="mission-vision">
            <div className="mission-vision__inner">
                <div className="mission__text" data-reveal>
                    <div className="mission__logo">
                        <span className="material-symbols-outlined">flag</span>
                    </div>
                    <h2 className="mission__title">Sứ mệnh</h2>
                    <p className="mission__desc">
                        Cung cấp một môi trường học tập trực tuyến dễ tiếp cận, có tính thực hành cao
                        và hỗ trợ người học phát triển kỹ năng lập trình thông qua khóa học, quiz,
                        bài tập code và phản hồi tự động.
                    </p>
                </div>
                <div className="vision__text" data-reveal style={{ "--reveal-delay": "110ms" }}>
                    <div className="vision__logo">
                        <span className="material-symbols-outlined">visibility</span>
                    </div>
                    <h2 className="vision__title">Tầm nhìn</h2>
                    <p className="vision__desc">
                        Trở thành một nền tảng LMS hoàn chỉnh cho đào tạo công nghệ, có thể mở rộng thêm AI,
                        gợi ý khóa học cá nhân hóa, gamification và các công cụ hỗ trợ giảng viên trong tương lai.
                    </p>
                </div>
            </div>
        </section>
    );
}
