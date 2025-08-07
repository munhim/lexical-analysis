import java.io.*;
import java.util.*;

class NfaState {
    int stateId;
    Map<Character, Set<NfaState>> transitions;

    public NfaState(int stateId) {
        this.stateId = stateId;
        this.transitions = new HashMap<>();
    }

    public void addTransition(char symbol, NfaState target) {
        Set<NfaState> targets = transitions.get(symbol);
        if (targets == null) {
            targets = new HashSet<>();
            transitions.put(symbol, targets);
        }
        targets.add(target);
    }
}

class Nfa {
    NfaState initial;
    NfaState finalState;

    public Nfa(NfaState initial, NfaState finalState) {
        this.initial = initial;
        this.finalState = finalState;
    }
}

class DfaState {
    int stateId;
    Map<Character, DfaState> transitions;
    boolean accepting;

    public DfaState(int stateId, boolean accepting) {
        this.stateId = stateId;
        this.transitions = new HashMap<>();
        this.accepting = accepting;
    }

    public void addTransition(char symbol, DfaState target) {
        transitions.put(symbol, target);
    }

    @Override
    public String toString() {
        return "DfaState{id=" + stateId + ", accepting=" + accepting + "}";
    }
}

class Dfa {
    DfaState startState;
    Set<DfaState> acceptingStates;

    public Dfa(DfaState startState, Set<DfaState> acceptingStates) {
        this.startState = startState;
        this.acceptingStates = acceptingStates;
    }
}

final class NfaFactory {
    public static final char EPSILON = 'ε';

    private NfaFactory() {
    }

    public static Nfa buildCharAutomaton(char c) {
        NfaState start = new NfaState(0);
        NfaState end = new NfaState(1);
        start.addTransition(c, end);
        return new Nfa(start, end);
    }

    public static Nfa concatenate(Nfa first, Nfa second) {
        first.finalState.addTransition(EPSILON, second.initial);
        return new Nfa(first.initial, second.finalState);
    }

    public static Nfa union(Nfa first, Nfa second) {
        NfaState newStart = new NfaState(0);
        NfaState newEnd = new NfaState(1);

        // add epsilon transitions from newStart to both first and second initial
        // states.
        NfaState[] initialStates = { first.initial, second.initial };
        for (NfaState state : initialStates) {
            newStart.addTransition(EPSILON, state);
        }
        // add epsilon transitions from both final states to newEnd.
        NfaState[] finalStates = { first.finalState, second.finalState };
        for (NfaState state : finalStates) {
            state.addTransition(EPSILON, newEnd);
        }
        return new Nfa(newStart, newEnd);
    }

    public static Nfa kleene(Nfa nfa) {
        NfaState newStart = new NfaState(0);
        NfaState newEnd = new NfaState(1);
        // from the new start state, add epsilon transitions to the original initial
        // state and new accept state.
        NfaState[] startTransitions = { nfa.initial, newEnd };
        for (NfaState state : startTransitions) {
            newStart.addTransition(EPSILON, state);
        }
        // from the original final state, add epsilon transitions back to the original
        // initial state and to the new accept state.
        NfaState[] finalTransitions = { nfa.initial, newEnd };
        for (NfaState state : finalTransitions) {
            nfa.finalState.addTransition(EPSILON, state);
        }
        return new Nfa(newStart, newEnd);
    }

    public static Nfa buildSequence(String sequence) {
        if (sequence.isEmpty()) {
            throw new IllegalArgumentException("Sequence cannot be empty.");
        }
        Nfa result = buildCharAutomaton(sequence.charAt(0));
        int i = 1;
        while (i < sequence.length()) {
            result = concatenate(result, buildCharAutomaton(sequence.charAt(i)));
            i++;
        }
        return result;
    }

    public static Nfa buildString(String str) {
        return buildSequence(str);
    }

    public static Nfa buildRange(char start, char end) {
        Nfa rangeNfa = buildCharAutomaton(start);
        char ch = (char) (start + 1);
        while (ch <= end) {
            rangeNfa = union(rangeNfa, buildCharAutomaton(ch));
            ch++;
        }
        return rangeNfa;
    }
}

final class NfaToDfaConverter {
    private NfaToDfaConverter() {
    }

    private static void epsilonClosureRecursive(NfaState state, Set<NfaState> closure) {
        for (NfaState next : state.transitions.getOrDefault(NfaFactory.EPSILON, Collections.emptySet())) {
            if (!closure.contains(next)) {
                closure.add(next);
                epsilonClosureRecursive(next, closure);
            }
        }
    }

    private static Set<NfaState> epsilonClosure(Set<NfaState> states) {
        Set<NfaState> closure = new HashSet<>(states);
        for (NfaState state : states) {
            epsilonClosureRecursive(state, closure);
        }
        return closure;
    }

    private static Set<NfaState> transition(Set<NfaState> states, char symbol) {
        Set<NfaState> result = new HashSet<>();
        for (NfaState state : states) {
            result.addAll(state.transitions.getOrDefault(symbol, Collections.emptySet()));
        }
        return result;
    }

    public static Dfa convertNfaToDfa(Nfa nfa) {
        int dfaId = 0;
        Map<Set<NfaState>, DfaState> stateMap = new HashMap<>();
        Queue<Set<NfaState>> queue = new LinkedList<>();

        Set<NfaState> startSet = epsilonClosure(Collections.singleton(nfa.initial));
        DfaState startDfa = new DfaState(dfaId++, startSet.contains(nfa.finalState));
        stateMap.put(startSet, startDfa);
        queue.add(startSet);

        while (!queue.isEmpty()) {
            Set<NfaState> currentSet = queue.poll();
            DfaState currentDfa = stateMap.get(currentSet);
            for (char symbol = 32; symbol <= 126; symbol++) {
                Set<NfaState> nextSet = epsilonClosure(transition(currentSet, symbol));
                if (!nextSet.isEmpty()) {
                    DfaState nextDfa = stateMap.get(nextSet);
                    if (nextDfa == null) {
                        nextDfa = new DfaState(dfaId++, nextSet.contains(nfa.finalState));
                        stateMap.put(nextSet, nextDfa);
                        queue.add(nextSet);
                    }
                    currentDfa.addTransition(symbol, nextDfa);
                }
            }
        }

        Set<DfaState> acceptingStates = new HashSet<>();
        for (DfaState ds : stateMap.values()) {
            if (ds.accepting) {
                acceptingStates.add(ds);
            }
        }
        return new Dfa(startDfa, acceptingStates);
    }
}

class NfaExpression {
    private final Nfa nfa;

    public NfaExpression(Nfa nfa) {
        this.nfa = nfa;
    }

    public static NfaExpression literal(char c) {
        return new NfaExpression(NfaFactory.buildCharAutomaton(c));
    }

    public NfaExpression concat(NfaExpression other) {
        return new NfaExpression(NfaFactory.concatenate(this.nfa, other.nfa));
    }

    public NfaExpression union(NfaExpression other) {
        return new NfaExpression(NfaFactory.union(this.nfa, other.nfa));
    }

    public NfaExpression star() {
        return new NfaExpression(NfaFactory.kleene(this.nfa));
    }

    public Nfa toNfa() {
        return this.nfa;
    }
}

class RegexPatterns {
    public static Nfa IdentifierNfa() {
        NfaExpression letters = null;
        for (char c = 'a'; c <= 'z'; c++) {
            NfaExpression letter = NfaExpression.literal(c);
            letters = (letters == null) ? letter : letters.union(letter);
        }
        return letters.concat(letters.star()).toNfa();
    }

    public static Nfa IntegerNfa() {
        NfaExpression digits = null;
        for (char c = '0'; c <= '9'; c++) {
            NfaExpression digit = NfaExpression.literal(c);
            digits = (digits == null) ? digit : digits.union(digit);
        }
        return digits.concat(digits.star()).toNfa();
    }

    public static Nfa DecimalNfa() {
        NfaExpression intPart = new NfaExpression(IntegerNfa());
        NfaExpression dot = NfaExpression.literal('.');
        NfaExpression fracPart = new NfaExpression(IntegerNfa());
        return intPart.concat(dot).concat(fracPart).toNfa();
    }

    public static Nfa BooleanNfa() {
        NfaExpression yes = new NfaExpression(NfaFactory.buildSequence("yes"));
        NfaExpression no = new NfaExpression(NfaFactory.buildSequence("no"));
        return yes.union(no).toNfa();
    }

    public static Nfa KeywordsNfa() {
        String[] keywords = { "number", "letter", "word", "choice", "decimal" };
        Nfa result = NfaFactory.buildSequence(keywords[0]);
        for (int i = 1; i < keywords.length; i++) {
            result = NfaFactory.union(result, NfaFactory.buildSequence(keywords[i]));
        }
        return result;
    }

    public static Nfa CharacterNfa() {
        Nfa openQuote = NfaFactory.buildString("'");
        Nfa contentNfa = IdentifierNfa();
        Nfa closeQuote = NfaFactory.buildString("'");
        Nfa temp = NfaFactory.concatenate(contentNfa, closeQuote);
        return NfaFactory.concatenate(openQuote, temp);
    }

    public static Nfa ArithmeticOperatorNfa() {
        String[] operators = { "add", "minus", "mul", "div", "pow", "barabar" };
        Nfa result = NfaFactory.buildSequence(operators[0]);
        for (int i = 1; i < operators.length; i++) {
            result = NfaFactory.union(result, NfaFactory.buildSequence(operators[i]));
        }
        return result;
    }

    public static Nfa ExponentNfa() {
        Nfa baseExp = DecimalNfa();
        Nfa caret = NfaFactory.buildCharAutomaton('^');
        Nfa exponent = DecimalNfa();
        return NfaFactory.concatenate(baseExp, NfaFactory.concatenate(caret, exponent));
    }

    public static Nfa SingleLineCommentNfa() {
        Nfa prefix = NfaFactory.buildSequence("bolo");
        Nfa rest = NfaFactory.kleene(NfaFactory.buildCharAutomaton('.'));
        return NfaFactory.concatenate(prefix, rest);
    }

    public static Nfa MultiLineCommentNfa() {
        Nfa start = NfaFactory.buildSequence("/*");
        Nfa body = NfaFactory.kleene(NfaFactory.buildCharAutomaton('.'));
        Nfa end = NfaFactory.buildSequence("*/");
        return NfaFactory.concatenate(start, NfaFactory.concatenate(body, end));
    }

    public static Nfa WhitespaceNfa() {
        char[] whitespaces = { ' ', '\t', '\n' };
        NfaExpression combined = null;
        for (char c : whitespaces) {
            NfaExpression ws = NfaExpression.literal(c);
            combined = (combined == null) ? ws : combined.union(ws);
        }
        return combined.star().toNfa();
    }

    public static Nfa StringNfa() {
        NfaExpression quote = NfaExpression.literal('"');
        NfaExpression content = new NfaExpression(NfaFactory.kleene(NfaFactory.buildCharAutomaton('.')));
        return quote.concat(content).concat(quote).toNfa();
    }

    public static Nfa InputOutputNfa() {
        Nfa inNfa = NfaFactory.buildSequence("andar");
        Nfa outNfa = NfaFactory.buildSequence("bahir");
        return NfaFactory.union(inNfa, outNfa);
    }

    public static Nfa ConstantNfa() {
        return new NfaExpression(NfaFactory.buildSequence("const")).toNfa();
    }
}

class SymbolTable {
    String name;
    String type;

    public SymbolTable(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SymbolTable))
            return false;
        SymbolTable that = (SymbolTable) o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}

class ErrorHandling {
    public static void reportError(int line, int pos, char unexpectedChar) {
        System.err.println("\u001B[31mError at Line " + line + ", Position " + pos + ": Unexpected character '"
                + unexpectedChar + "'\u001B[0m");
    }

    public static void reportUnclosedMultiLineComment(int startLine) {
        System.err.println("\u001B[31mError: Unclosed multi-line comment starting at Line " + startLine + "\u001B[0m");
    }

    public static void reportUnclosedParenthesis() {
        System.err.println("\u001B[31mError: Unclosed parenthesis detected.\u001B[0m");
    }

    public static void reportUnclosedString(int startLine) {
        System.err.println("\u001B[31mError: Unclosed string literal starting at Line " + startLine + "\u001B[0m");
    }
}

public class CodeTokenizer {
    private final Map<String, Dfa> s;
    private final Set<SymbolTable> symbols;

    public CodeTokenizer() {
        s = new HashMap<>();
        symbols = new LinkedHashSet<>();
        s.put("IDENTIFIER", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.IdentifierNfa()));
        s.put("KEYWORD", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.KeywordsNfa()));
        s.put("INTEGER", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.IntegerNfa()));
        s.put("DECIMAL", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.DecimalNfa()));
        s.put("BOOLEAN", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.BooleanNfa()));
        s.put("CHARACTER", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.CharacterNfa()));
        s.put("OPERATOR", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.ArithmeticOperatorNfa()));
        s.put("EXPONENT", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.ExponentNfa()));
        s.put("SINGLE_LINE_COMMENT", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.SingleLineCommentNfa()));
        s.put("MULTI_LINE_COMMENT", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.MultiLineCommentNfa()));
        s.put("WHITESPACE", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.WhitespaceNfa()));
        s.put("STRING", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.StringNfa()));
        s.put("IO", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.InputOutputNfa()));
        s.put("CONSTANT", NfaToDfaConverter.convertNfaToDfa(RegexPatterns.ConstantNfa()));
        addPredefinedSymbols();
    }

    private void addPredefinedSymbols() {
        // Keywords
        String[] keywords = { "number", "letter", "word", "choice", "decimal" };
        for (String kw : keywords) {
            symbols.add(new SymbolTable(kw, "KEYWORD"));
        }

        // Boolean values
        symbols.add(new SymbolTable("yes", "BOOLEAN"));
        symbols.add(new SymbolTable("no", "BOOLEAN"));

        // Operators
        String[] operators = { "add", "minus", "mul", "div", "pow", "barabar" };
        for (String op : operators) {
            symbols.add(new SymbolTable(op, "OPERATOR"));
        }

        // Input/Output
        symbols.add(new SymbolTable("andar", "IO"));
        symbols.add(new SymbolTable("bahir", "IO"));

        // Constants
        symbols.add(new SymbolTable("const", "CONSTANT"));

        // Literal Patterns
        symbols.add(new SymbolTable("\\d+\\.\\d{1,5}", "DECIMAL_PATTERN"));
        symbols.add(new SymbolTable("'[a-z]|[A-Z]'", "CHARACTER_PATTERN"));
        symbols.add(new SymbolTable("\".*?\"", "STRING_PATTERN"));
    }

    public List<String> tokenize(String code) {
        List<String> tokens = new ArrayList<>();
        int pos = 0;
        int currentLine = 1;
        int openParentheses = 0;
        boolean inMultilineComment = false;
        boolean inString = false;
        int multilineCommentStart = -1;
        int stringStartLine = -1;
        StringBuilder stringContent = new StringBuilder();

        while (pos < code.length()) {
            boolean tokenMatched = false;

            if (code.charAt(pos) == '\n') {
                currentLine++;
                pos++;
                continue;
            }

            // Multi-line comment check
            if (code.startsWith("zadabolo", pos)) {
                if (inMultilineComment) {
                    inMultilineComment = false;
                    tokens.add("MULTI_LINE_COMMENT_END: zadabolo");
                } else {
                    inMultilineComment = true;
                    multilineCommentStart = currentLine;
                    tokens.add("MULTI_LINE_COMMENT_START: zadabolo");
                }
                pos += "zadabolo".length();
                tokenMatched = true;
                continue;
            }

            if (inMultilineComment) {
                pos++; // Consume characters inside comment
                continue;
            }

            // String completion check (fix to capture content inside quotes)
            if (code.charAt(pos) == '"') {
                if (inString) {
                    tokens.add("STRING: \"" + stringContent.toString() + "\"");
                    stringContent.setLength(0); // Clear string builder
                    inString = false;
                } else {
                    inString = true;
                    stringStartLine = currentLine;
                }
                pos++;
                tokenMatched = true;
                continue;
            }

            if (inString) {
                stringContent.append(code.charAt(pos));
                pos++;
                continue;
            }

            // Parenthesis check
            if (code.charAt(pos) == '(') {
                openParentheses++;
                tokens.add("PARENTHESIS_OPEN: (");
                pos++;
                tokenMatched = true;
                continue;
            }

            if (code.charAt(pos) == ')') {
                if (openParentheses == 0) {
                    ErrorHandling.reportError(currentLine, pos, ')');
                } else {
                    openParentheses--;
                }
                tokens.add("PARENTHESIS_CLOSE: )");
                pos++;
                tokenMatched = true;
                continue;
            }

            // DFA Matching
            for (Map.Entry<String, Dfa> entry : s.entrySet()) {
                String type = entry.getKey();
                Dfa dfa = entry.getValue();
                int len = matchDfa(dfa, code, pos, type);
                if (len > 0) {
                    String token = code.substring(pos, pos + len);
                    pos += len;
                    if (!type.equals("WHITESPACE")) {
                        tokens.add(type + ": " + token);
                    }
                    tokenMatched = true;
                    break;
                }
            }

            if (!tokenMatched) {
                ErrorHandling.reportError(currentLine, pos, code.charAt(pos));
                pos++;
            }
        }

        // Final error checks
        if (inMultilineComment) {
            ErrorHandling.reportUnclosedMultiLineComment(multilineCommentStart);
        }
        if (openParentheses > 0) {
            ErrorHandling.reportUnclosedParenthesis();
        }
        if (inString) {
            ErrorHandling.reportUnclosedString(stringStartLine);
        }

        return tokens;
    }

    private int matchDfaRecursive(DfaState current, String input, int pos, int length, int lastAcceptLength,
            List<String> transitionsList) {
        if (pos >= input.length())
            return lastAcceptLength;
        char ch = input.charAt(pos);
        if (current.transitions.containsKey(ch)) {
            DfaState next = current.transitions.get(ch);
            transitionsList.add("State " + current.stateId + " --'" + ch + "'--> State " + next.stateId);
            int newLength = length + 1;
            if (next.accepting) {
                lastAcceptLength = newLength;
            }
            return matchDfaRecursive(next, input, pos + 1, newLength, lastAcceptLength, transitionsList);
        }
        return lastAcceptLength;
    }

    private int matchDfa(Dfa dfa, String input, int startPos, String type) {
        List<String> transitionsList = new ArrayList<>();
        int lastAcceptLength = matchDfaRecursive(dfa.startState, input, startPos, 0, 0, transitionsList);
        if (!type.equals("WHITESPACE") && lastAcceptLength > 0) {
            System.out.println("\nTransition Table for token: " +
                    input.substring(startPos, startPos + lastAcceptLength));
            for (String trans : transitionsList) {
                System.out.println(trans);
            }
            System.out.println();
        }
        return lastAcceptLength;
    }

    public void displaySymbolTable() {
        System.out.println("\nSymbol Table:");
        System.out.println("-------------------------------------------------");
        System.out.printf("| %-20s | %-18s |\n", "Identifier", "Type");
        System.out.println("-------------------------------------------------");

        for (SymbolTable entry : symbols) {
            System.out.printf("| %-20s | %-18s |\n", entry.name, entry.type);
        }

        System.out.println("-------------------------------------------------");
    }

    public static void main(String[] args) {
        CodeTokenizer tokenizer = new CodeTokenizer();
        StringBuilder sourceCode = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("code.mi"))) {
            String line = br.readLine();
            while (line != null) {
                sourceCode.append(line).append("\n");
                line = br.readLine();
            }
        } catch (IOException ex) {
            System.err.println("Error reading file: " + ex.getMessage());
            return;
        }

        List<String> tokens = tokenizer.tokenize(sourceCode.toString());
        for (String t : tokens) {
            System.out.println(t.toLowerCase());
        }
        tokenizer.displaySymbolTable();
    }
}