import React, { createContext, useContext, useState } from 'react';

const ProblemContext = createContext(null);

export function ProblemProvider({ children }) {
    const [problemId, setProblemId] = useState(null);
    return (
        <ProblemContext.Provider value={{ problemId, setProblemId }}>
            {children}
        </ProblemContext.Provider>
    );
}

export function useProblemContext() {
    return useContext(ProblemContext);
}
