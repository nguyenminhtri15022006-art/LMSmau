import React, {useCallback, useEffect, useRef, useState} from 'react';
import {basicSetup} from 'codemirror';
import {EditorState} from '@codemirror/state';
import {EditorView, keymap} from '@codemirror/view';
import {indentWithTab} from '@codemirror/commands';
import {oneDark} from '@codemirror/theme-one-dark';

const LANG_EXTENSIONS = {
    javascript: async () => {
        const {javascript} = await import('@codemirror/lang-javascript');
        return javascript();
    },
    typescript: async () => {
        const {javascript} = await import('@codemirror/lang-javascript');
        return javascript({typescript: true});
    },
    python: async () => {
        const {python} = await import('@codemirror/lang-python');
        return python();
    },
    java: async () => {
        const {java} = await import('@codemirror/lang-java');
        return java();
    },
    cpp: async () => {
        const {cpp} = await import('@codemirror/lang-cpp');
        return cpp();
    },
    c: async () => {
        const {cpp} = await import('@codemirror/lang-cpp');
        return cpp();
    },
    rust: async () => {
        const {rust} = await import('@codemirror/lang-rust');
        return rust();
    },
    go: async () => {
        const {go} = await import('@codemirror/lang-go');
        return go();
    },
    html: async () => {
        const {html} = await import('@codemirror/lang-html');
        return html();
    },
    css: async () => {
        const {css} = await import('@codemirror/lang-css');
        return css();
    },
};

function getLanguageExtension(language) {
    const ext = LANG_EXTENSIONS[language];
    return ext ? ext() : LANG_EXTENSIONS.javascript();
}

const FONT_FAMILY = '"JetBrains Mono", "Fira Code", "Cascadia Code", "Monaco", "Courier New", monospace';

export default function CodeEditor({code, setCode, language}) {
    const editorRef = useRef(null);
    const viewRef = useRef(null);
    const codeRef = useRef(code);
    const [isLoadingLanguage, setIsLoadingLanguage] = useState(true);

    useEffect(() => {
        codeRef.current = code;
    }, [code]);

    const handleChange = useCallback(
        (update) => {
            if (update.docChanged) {
                const current = update.state.doc.toString();
                if (current !== codeRef.current) {
                    codeRef.current = current;
                    setCode(current);
                }
            }
        },
        [setCode],
    );

    useEffect(() => {
        if (!editorRef.current) return;

        let isCancelled = false;
        setIsLoadingLanguage(true);

        async function createEditor() {
            const languageExtension = await getLanguageExtension(language);

            if (isCancelled || !editorRef.current) return;

            if (viewRef.current) {
                viewRef.current.destroy();
            }

            const extensions = [
                basicSetup,
                oneDark,
                keymap.of([indentWithTab]),
                languageExtension,
                EditorView.updateListener.of(handleChange),
                EditorView.theme(
                    {
                        '&': {
                            height: '100%',
                            fontSize: '14px',
                            fontFamily: FONT_FAMILY,
                            backgroundColor: '#1e293b',
                        },
                        '.cm-scroller': {
                            overflow: 'auto',
                            lineHeight: '1.7',
                            height: '100%',
                        },
                        '.cm-content': {
                            minWidth: 'max-content',
                            padding: '20px 24px',
                            caretColor: '#e2e8f0',
                            cursor: 'text',
                        },
                        '.cm-cursor': {
                            borderLeftColor: '#e2e8f0',
                        },
                        '.cm-activeLine': {
                            backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        },
                        '.cm-selectionBackground': {
                            backgroundColor: 'rgba(37, 99, 235, 0.3) !important',
                        },
                        '.cm-gutters': {
                            backgroundColor: '#0f172a',
                            borderRight: '1px solid #334155',
                            color: '#475569',
                        },
                        '.cm-activeLineGutter': {
                            backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        },
                        '.cm-matchingBracket': {
                            backgroundColor: 'rgba(37, 99, 235, 0.2)',
                            outline: '1px solid #3b82f6',
                        },
                        '.cm-tooltip': {
                            backgroundColor: '#1e293b',
                            border: '1px solid #334155',
                            borderRadius: '6px',
                            color: '#e2e8f0',
                        },
                    },
                    {dark: true},
                ),
            ];

            const state = EditorState.create({
                doc: codeRef.current,
                extensions,
            });

            const view = new EditorView({
                state,
                parent: editorRef.current,
            });

            viewRef.current = view;
            setIsLoadingLanguage(false);
        }

        createEditor();

        return () => {
            isCancelled = true;
            if (viewRef.current) {
                viewRef.current.destroy();
                viewRef.current = null;
            }
        };
    }, [language, handleChange]);

    useEffect(() => {
        if (!viewRef.current) return;
        const currentDoc = viewRef.current.state.doc.toString();
        if (code !== currentDoc) {
            viewRef.current.dispatch({
                changes: {from: 0, to: currentDoc.length, insert: code},
            });
        }
    }, [code]);

    return (
        <div className="code-editor-shell">
            <div ref={editorRef} className="code-editor-host"/>
            {isLoadingLanguage && <div className="code-editor-loading">Loading language...</div>}
        </div>
    );
}
