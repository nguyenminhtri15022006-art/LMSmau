const submissions = db.getCollection("submissions");

const statusMappings = {
  ACCEPTED: "AC",
  WRONG_ANSWER: "WA",
  TIME_LIMIT: "TLE",
  TIME_LIMIT_EXCEEDED: "TLE",
  MEMORY_LIMIT: "MLE",
  MEMORY_LIMIT_EXCEEDED: "MLE",
  RUNTIME_ERROR: "RE",
  COMPILATION_ERROR: "CE",
  INTERNAL_ERROR: "IE",
};

Object.entries(statusMappings).forEach(([legacyStatus, canonicalStatus]) => {
  submissions.updateMany(
    { status: legacyStatus },
    { $set: { status: canonicalStatus } }
  );

  submissions.updateMany(
    { "test_case_result.status": legacyStatus },
    { $set: { "test_case_result.$[testCase].status": canonicalStatus } },
    { arrayFilters: [{ "testCase.status": legacyStatus }] }
  );
});

[
  "idx_user_problem_time",
  "idx_user_course_score",
  "idx_problem_status",
  "idx_pending_poll",
].forEach((indexName) => {
  try {
    submissions.dropIndex(indexName);
  } catch (error) {
    if (error.codeName !== "IndexNotFound") {
      throw error;
    }
  }
});

submissions.createIndex(
  { user_id: 1, problem_id: 1, submitted_at: -1 },
  { name: "idx_user_problem_time" }
);
submissions.createIndex(
  { user_id: 1, course_id: 1, score: -1 },
  { name: "idx_user_course_score" }
);
submissions.createIndex(
  { problem_id: 1, status: 1 },
  { name: "idx_problem_status" }
);
submissions.createIndex(
  { status: 1, submitted_at: 1 },
  { name: "idx_pending_poll" }
);

const legacyStatuses = Object.keys(statusMappings);
const remainingLegacyStatusCount = submissions.countDocuments({
  $or: [
    { status: { $in: legacyStatuses } },
    { "test_case_result.status": { $in: legacyStatuses } },
  ],
});

print(`remainingLegacyStatusCount=${remainingLegacyStatusCount}`);
