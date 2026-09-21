import "../../styles/us/KeyStats.css";

export default function KeyStats() {
    return (
        <section className="key-stats">
            <div className="key-stats__inner">
                <div className="key-stats__item">
                    <div className="key-stats__logo">
                        <span className="material-symbols-outlined">
                            groups
                        </span>
                    </div>
                    <h3 className="key-stats__title">500+</h3>
                    <p className="key-stats__desc">Sinh viên</p>
                </div>
                <div className="key-stats__item">
                    <div className="key-stats__logo">
                        <span className="material-symbols-outlined">
                            school
                        </span>
                    </div>
                    <h3 className="key-stats__title">100+</h3>
                    <p className="key-stats__desc">Khóa học</p>
                </div>
                <div className="key-stats__item">
                    <div className="key-stats__logo">
                        <span className="material-symbols-outlined">
                            star
                        </span>
                    </div>
                    <h3 className="key-stats__title">100+</h3>
                    <p className="key-stats__desc">Giảng viên</p>
                </div>
            </div>
        </section>
    );
}