import React from 'react';
import "../../../styles/student/ExerciseHub/ExerciseHero.css";

const ExerciseHero = ({
  title = "Nâng cao kỹ năng thông qua thực hành",
  subtitle = "Khám phá bộ sưu tập các bài tập lập trình và câu đố tương tác được tuyển chọn kỹ lưỡng, giúp củng cố kiến thức và nâng cao sự tự tin của bạn.",
  buttonText = "Bắt đầu thực hành ngay",
  onButtonClick = () => { }
}) => {
  return (
    <section className="exercise-hero">
      <h1>{title}</h1>
      <p>{subtitle}</p>
      {buttonText && <button className="start-btn" onClick={onButtonClick}>{buttonText}</button>}
    </section>
  );
};

export default ExerciseHero;
