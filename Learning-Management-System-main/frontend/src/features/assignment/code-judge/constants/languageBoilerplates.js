export const POPULAR_LANGUAGES = [
  { id: 'python', name: 'Python', version: '3.13', icon: '🐍' },
  { id: 'javascript', name: 'JavaScript', version: 'ES2024', icon: '📜' },
  { id: 'typescript', name: 'TypeScript', version: '5.8', icon: '🔷' },
  { id: 'java', name: 'Java', version: '21 LTS', icon: '☕' },
  { id: 'cpp', name: 'C++', version: 'C++23', icon: '⚡' },
  { id: 'c', name: 'C', version: 'C23', icon: '🔌' },
  { id: 'rust', name: 'Rust', version: '1.88', icon: '🦀' },
  { id: 'go', name: 'Go', version: '1.24', icon: '🐹' },
];

export const BOILERPLATE_CODE = {
  python: `# Python 3
def solve() -> None:
    # Viết code của bạn ở đây
    pass

if __name__ == '__main__':
    solve()
`,
  javascript: `// JavaScript (Node.js)
function solve() {
    // Viết code của bạn ở đây
    console.log("Hello, World!");
}

solve();
`,
  typescript: `// TypeScript
function solve(): void {
    // Viết code của bạn ở đây
    console.log("Hello, World!");
}

solve();
`,
  java: `// Java
public class Solution {
    public static void main(String[] args) {
        // Viết code của bạn ở đây
        System.out.println("Hello, World!");
    }
}
`,
  cpp: `// C++
#include <iostream>
using namespace std;

int main() {
    // Viết code của bạn ở đây
    cout << "Hello, World!" << endl;
    return 0;
}
`,
  c: `// C
#include <stdio.h>

int main() {
    // Viết code của bạn ở đây
    printf("Hello, World!\\n");
    return 0;
}
`,
  rust: `// Rust
fn main() {
    // Viết code của bạn ở đây
    println!("Hello, World!");
}
`,
  go: `// Go
package main

import "fmt"

func main() {
    // Viết code của bạn ở đây
    fmt.Println("Hello, World!")
}
`,
  html: `<!-- HTML -->
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Bài tập</title>
</head>
<body>
    <!-- Viết code của bạn ở đây -->
</body>
</html>
`,
  css: `/* CSS */
body {
    /* Viết code của bạn ở đây */
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}
`,
};

export function getBoilerplateCode(languageId) {
  return BOILERPLATE_CODE[languageId] || BOILERPLATE_CODE.python;
}

export function getLanguageById(languageId) {
  return POPULAR_LANGUAGES.find((l) => l.id === languageId) || POPULAR_LANGUAGES[0];
}
