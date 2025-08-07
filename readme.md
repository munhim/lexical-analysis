

# mi+ Language – Lexical Specification and User Manual

The **mi+** language is designed with a custom regex engine built from scratch. Instead of using Java’s built‑in regex libraries, the language constructs its own patterns by building NFAs (Non‑deterministic Finite Automata) for each token type, which are then converted to DFAs (Deterministic Finite Automata) for efficient matching.

This README explains all the token types, their rules, and the reserved keywords for each type. It also provides instructions on how to compile and run the lexer.

---

## Reserved Keywords and Their Meanings

The following reserved keywords are used in **mi+** to declare data types and control language constructs:

- **number**  
  *Declares a numeric variable.*  
  **Example:**  
  ```mi+
  number x
  ```

- **letter**  
  *Declares a character variable.*  
  **Example:**  
  ```mi+
  letter ch
  ```

- **word**  
  *Declares a string variable.*  
  **Example:**  
  ```mi+
  word name
  ```

- **choice**  
  *Used for enumerated types or selection constructs.*  
  **Example:**  
  ```mi+
  choice option 
  ```

- **decimal**  
  *Declares a floating‑point (decimal) variable.*  
  **Example:**  
  ```mi+
  decimal price 
  ```

- **const**  
  *Declares a constant value.*  
  **Example:**  
  ```mi+
  const PI = 3.14 
  ```

- **andar** and **bahir**  
  *Reserved for input/output operations (andar for input and bahir for output).*  
  **Example:**  
  ```mi+
  andar inputValue 
  bahir outputValue 
  ```

- **bolo**  
  *Marks the beginning of a single-line comment.*  
  **Example:**  
  ```mi+
  bolo This is a single-line comment
  ```

- **zyadabolo**  
  *Marks the beginning and end of a multi-line comment.*  
  **Example:**  
  ```mi+
  zyadabolo
  This is a multi-line comment.
  It spans several lines.
  zyadabolo
  ```

---

## Token Types and Rules

Each token in **mi+** is defined by a custom NFA pattern that is converted to a DFA. The following are the token types with their rules, descriptions, examples, and the associated reserved keywords when applicable.

### 1. IDENTIFIER
- **Rule:** A sequence of one or more lowercase letters (`a`–`z`).
- **Usage:** Used for variable names and user-defined symbols.
- **Examples:** `abc`, `name`
- **Pattern:** `[a-z]+`

---

### 2. INTEGER
- **Rule:** A sequence of one or more digits.
- **Usage:** Represents whole-number constants.
- **Examples:** `123`, `0`
- **Pattern:** `\d+`

---

### 3. DECIMAL
- **Rule:** One or more digits, a dot (`.`), followed by one or more digits (fractional part limited to 1–5 digits).
- **Usage:** Represents floating‑point numbers.
- **Examples:** `12.34`, `0.001`
- **Pattern:** `\d+\.\d{1,5}`

---

### 4. BOOLEAN
- **Rule:** The literal words `yes` or `no`.
- **Usage:** Represents Boolean values.
- **Examples:** `yes`, `no`
- **Pattern:** `(yes|no)`

---

### 5. CHARACTER
- **Rule:** A single character literal enclosed in single quotes.
- **Usage:** Used for declaring character variables (must be declared using the keyword **letter**).
- **Examples:** `'a'`, `'z'`
- **Pattern:** An opening single quote, a letter (as defined by the IDENTIFIER pattern), and a closing single quote.

---

### 6. OPERATOR
- **Rule:** A set of arithmetic operator keywords.
- **Usage:** Represents arithmetic operations.
- **Examples:** `add`, `minus`, `mul`, `div`, `pow`, `barabar`
- **Pattern:** `(add|minus|mul|div|pow|barabar)`

---

### 7. KEYWORDS
- **Rule:** Reserved words that define the data types and constructs.
- **Usage:**  
  - **number:** Declares numeric variables.  
  - **letter:** Declares character variables.  
  - **word:** Declares string variables.  
  - **choice:** For enumerated types or selection constructs.  
  - **decimal:** Declares floating‑point variables.
- **Examples:** `number`, `letter`, `word`, `choice`, `decimal`
- **Pattern:** The union of these literal strings.

---

### 8. EXPONENT
- **Rule:** A decimal constant, followed by a caret (`^`), followed by another decimal constant.
- **Usage:** Represents exponentiation.
- **Example:** `3.14^2.71`
- **Pattern:** A concatenation of a DECIMAL, the literal `^`, and another DECIMAL.

---

### 9. SINGLE_LINE_COMMENT
- **Rule:** A comment starting with the literal `bolo` and continuing until the end of the line.
- **Usage:** For inline comments.
- **Example:** `bolo This is a comment`
- **Pattern:** The literal `"bolo"` followed by any sequence of characters until a newline.

---

### 10. MULTI_LINE_COMMENT
- **Rule:** A comment that starts and ends with the keyword `zyadabolo`.
- **Usage:** Used for comments that span multiple lines.
- **Example:**
  ```mi+
  zyadabolo
  This is a multi-line comment.
  It spans several lines.
  zyadabolo
  ```
- **Pattern:** The content between two occurrences of `zyadabolo`.

---

### 11. WHITESPACE
- **Rule:** One or more spaces, tabs, or newline characters.
- **Usage:** Separates tokens (generally ignored during parsing).
- **Pattern:** A union of space, tab, and newline characters, with the Kleene star applied.

---

### 12. STRING
- **Rule:** A sequence of characters enclosed in double quotes.
- **Usage:** Represents string literals (should be declared using the keyword **word**).
- **Example:** `"Hello, world!"`
- **Pattern:** A double quote, followed by any sequence of characters, and ending with a double quote.

---

### 13. IO
- **Rule:** Reserved keywords for input/output operations.
- **Usage:**  
  - **andar:** Indicates input.  
  - **bahir:** Indicates output.
- **Examples:** `andar`, `bahir`
- **Pattern:** `(andar|bahir)`

---

### 14. CONSTANT
- **Rule:** The literal keyword `const`.
- **Usage:** Declares constants.
- **Example:** `const`
- **Pattern:** The literal string `"const"`

---

## How to Compile and Run

1. **Compilation:**  
   Open a terminal in the directory containing your Java files and compile with:
   ```bash
   javac CodeTokenizer.java
   ```

2. **Execution:**  
   Ensure that your source code is in a file named `code.mi` and run:
   ```bash
   java CodeTokenizer
   ```

3. **Output:**  
   The lexer will print each token (in lowercase) and then display a symbol table listing all identifiers along with their associated types.

---

## Implementation Details

- **Custom Regex Engine:**  
  The **mi+** language uses a self‑made regex engine. Each token type is defined by constructing an NFA using our own methods (in `NfaFactory` and `RegexPatterns`). These NFAs are then converted into DFAs by `NfaToDfaConverter` for efficient pattern matching.

- **Token Matching:**  
  The `CodeTokenizer` class processes the source code, matching tokens using the DFAs for each token type. Special cases (such as string literals, parentheses, and multi‑line comments) are handled separately.

- **Symbol Table:**  
  Identifiers declared with reserved keywords (like **letter** for character variables and **word** for string variables) are stored in a symbol table. This table is used later for semantic analysis.

- **Reserved Keywords Summary:**  
  - **number:** Numeric variable declaration.  
  - **letter:** Character variable declaration.  
  - **word:** String variable declaration.  
  - **choice:** For enumerated types.  
  - **decimal:** Floating‑point variable declaration.  
  - **const:** Constant declaration.  
  - **andar / bahir:** For input/output operations.  
  - **bolo:** Marks the start of a single-line comment.  
  - **zyadabolo:** Used as the marker to start and end a multi-line comment.

---

## Conclusion

This README provides a complete overview of the **mi+** language’s lexical structure. It details every token type and the reserved keywords for data types and language constructs, and it explains how to compile and run the lexer. Use this document as a reference for writing and implementing programs in **mi+**.

---

