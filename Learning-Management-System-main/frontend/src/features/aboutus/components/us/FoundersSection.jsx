import "../../styles/us/FoundersSection.css";
import { DEFAULT_FOUNDERS_CONTENT } from "../../constants/foundersContent";

export default function FoundersSection() {
    const content = DEFAULT_FOUNDERS_CONTENT;

    return (
        <section className="founders-section">
            <div className="founders-section__inner">
                <div className="founders-section__header" data-reveal>
                    <span>{content.eyebrow}</span>
                    <h2>{content.title}</h2>
                    <p>{content.description}</p>
                </div>

                <div className="founders-section__grid">
                    {content.founders.map((founder, index) => (
                        <article
                            className="founder-card"
                            key={founder.id}
                            data-reveal
                            style={{ "--reveal-delay": `${index * 70}ms` }}
                        >
                            <div className="founder-card__avatar-wrap">
                                <img
                                    className="founder-card__avatar"
                                    src={founder.avatar}
                                    alt={`Ảnh đại diện của ${founder.name}`}
                                    loading="lazy"
                                />
                            </div>
                            <div className="founder-card__content">
                                <h3>{founder.name}</h3>
                                <p>{founder.role}</p>
                            </div>
                        </article>
                    ))}
                </div>
            </div>
        </section>
    );
}
