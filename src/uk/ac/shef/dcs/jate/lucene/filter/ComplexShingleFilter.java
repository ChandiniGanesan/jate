package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.util.AttributeSource;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by - on 12/10/2015.
 */
public class ComplexShingleFilter extends TokenFilter {
    /**
     * filler token for when positionIncrement is more than 1
     */
    public static final String DEFAULT_FILLER_TOKEN = "_";

    /**
     * default maximum shingle size is 2.
     */
    public static final int DEFAULT_MAX_SHINGLE_SIZE = 2;

    /**
     * default minimum shingle size is 2.
     */
    public static final int DEFAULT_MIN_SHINGLE_SIZE = 2;

    /**
     * default maximum shingle size is 2.
     */
    public static final int DEFAULT_MAX_CHAR_LENGTH = 50;

    /**
     * default minimum shingle size is 2.
     */
    public static final int DEFAULT_MIN_CHAR_LENGTH = 1;

    /**
     * given "the cat sat on the" if this is true, and "the" is a stopword it becomes "cat sat on the"
     */
    public static final boolean DEFAULT_REMOVE_LEADING_STOPWORDS = false;

    /**
     * given "the cat sat on the" if this is true, and "the" is a stopword it becomes "the cat sat on"
     */
    public static final boolean DEFAULT_REMOVE_TRAILING_STOPWORDS = false;

    /**
     * if true, token n-gram will not created across sentences.
     */
    public static final boolean DEFAULT_SENTENCE_BOUNDARY_AWARE = true;


    /**
     * if true, given ", my house +" this becomes "my house +"
     */
    public static final boolean DEFAULT_REMOVE_LEADING_SYMBOLS = false;


    /**
     * if true, given ", my house +" this becomes ", my house"
     */
    public static final boolean DEFAULT_REMOVE_TRAILING_SYMBOLS = false;


    /**
     * default token type attribute value is "shingle"
     */
    public static final String DEFAULT_TOKEN_TYPE = "shingle";

    /**
     * The default string to use when joining adjacent tokens to form a shingle
     */
    public static final String DEFAULT_TOKEN_SEPARATOR = " ";

    /**
     * The sequence of input stream tokens (or filler tokens, if necessary)
     * that will be composed to form output shingles.
     */
    private LinkedList<InputWindowToken> inputWindow
            = new LinkedList<>();

    /**
     * The number of input tokens in the next output token.  This is the "n" in
     * "token n-grams".
     */
    private CircularSequence gramSize;

    /**
     * Shingle and unigram text is composed here.
     */
    private StringBuilder gramBuilder = new StringBuilder();

    /**
     * The token type attribute value to use - default is "shingle"
     */
    private String tokenType = DEFAULT_TOKEN_TYPE;

    /**
     * The string to use when joining adjacent tokens to form a shingle
     */
    private String tokenSeparator = DEFAULT_TOKEN_SEPARATOR;

    /**
     * The string to insert for each position at which there is no token
     * (i.e., when position increment is greater than one).
     */
    private char[] fillerToken = DEFAULT_FILLER_TOKEN.toCharArray();

    /**
     * By default, we output unigrams (individual tokens) as well as shingles
     * (token n-grams).
     */
    private boolean outputUnigrams = true;

    /**
     * By default, we don't override behavior of outputUnigrams.
     */
    private boolean outputUnigramsIfNoShingles = false;

    /**
     * maximum shingle size (number of tokens)
     */
    private int maxShingleSize;

    /**
     * minimum shingle size (number of tokens)
     */
    private int minShingleSize;

    /**
     * maximum shingle size (number of tokens)
     */
    private int maxCharLength;

    /**
     * minimum shingle size (number of tokens)
     */
    private int minCharLength;

    private boolean removeLeadingStopwords;
    private boolean removeTrailingStopwords;
    private boolean sentenceBoundaryAware;
    private boolean removeLeadingSymbolicTokens;
    private boolean removeTrailingSymbolicTokens;

    /**
     * The remaining number of filler tokens to be inserted into the input stream
     * from which shingles are composed, to handle position increments greater
     * than one.
     */
    private int numFillerTokensToInsert;

    /**
     * When the next input stream token has a position increment greater than
     * one, it is stored in this field until sufficient filler tokens have been
     * inserted to account for the position increment.
     */
    private AttributeSource nextInputStreamToken;

    /**
     * Whether or not there is a next input stream token.
     */
    private boolean isNextInputStreamToken = false;

    /**
     * Whether at least one unigram or shingle has been output at the current
     * position.
     */
    private boolean isOutputHere = false;

    /**
     * true if no shingles have been output yet (for outputUnigramsIfNoShingles).
     */
    boolean noShingleOutput = true;

    boolean stopWordsIgnoreCase=false;

    private Set<String> stopWords;


    /**
     * Holds the State after input.end() was called, so we can
     * restore it in our end() impl.
     */
    private State endState;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final FlagsAttribute sentenceBoundaryFlag = addAttribute(FlagsAttribute.class);
    /**
     * A flag used to control Shingle emitting behaviour. If a sentence boundary token is found in the previous window generated
     * this var will be the index of that token in that window
     */
    private int sentenceBoundaryInWindow = -1;

    /**
     * Constructs a ShingleFilter with the specified shingle size from the
     * {@link TokenStream} <code>input</code>
     *
     * @param input          input stream
     * @param minShingleSize minimum shingle size produced by the filter.
     * @param maxShingleSize maximum shingle size produced by the filter.
     */
    public ComplexShingleFilter(TokenStream input, int minShingleSize, int maxShingleSize,
                                int minCharLength, int maxCharLength,
                                boolean outputUnigrams, boolean outputUnigramsIfNoShingles,
                                String tokenSeparater, String fillerToken,
                                boolean removeLeadingStopWords,
                                boolean removeTrailingStopwords,
                                boolean removeLeadingSymbolicTokens,
                                boolean removeTrailingSymbolicTokens,
                                boolean sentenceBoundaryAware,
                                Set<String> stopWords,
                                boolean stopWordsIgnoreCase) {
        super(input);
        this.minShingleSize = minShingleSize;
        this.maxShingleSize = maxShingleSize;
        this.minCharLength = minCharLength;
        this.maxCharLength = maxCharLength;
        this.outputUnigrams = outputUnigrams;
        this.outputUnigramsIfNoShingles = outputUnigramsIfNoShingles;
        this.tokenSeparator = tokenSeparater;
        this.fillerToken = fillerToken.toCharArray();
        this.removeLeadingStopwords = removeLeadingStopWords;
        this.removeTrailingStopwords = removeTrailingStopwords;
        this.removeLeadingSymbolicTokens = removeLeadingSymbolicTokens;
        this.removeTrailingSymbolicTokens = removeTrailingSymbolicTokens;
        this.sentenceBoundaryAware = sentenceBoundaryAware;
        this.stopWords = stopWords;
        gramSize = new CircularSequence();
        this.stopWordsIgnoreCase=stopWordsIgnoreCase;
    }


    /**
     * Set the type of the shingle tokens produced by this filter.
     * (default: "shingle")
     *
     * @param tokenType token tokenType
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }


    @Override
    public boolean incrementToken() throws IOException {
        boolean tokenAvailable = false;
        int builtGramSize = 0;

        if (gramSize.atMinValue() || inputWindow.size() < gramSize.getValue()) {
            shiftInputWindow();
            if(sentenceBoundaryAware)
                sentenceBoundaryInWindow = findSentenceBoundary();
            gramBuilder.setLength(0);
        } else {
            builtGramSize = gramSize.getPreviousValue();
        }
        if (inputWindow.size() >= gramSize.getValue()) {
            boolean isAllFiller = true;
            InputWindowToken nextToken = null;
            Iterator<InputWindowToken> iter = inputWindow.iterator();

            int idxInWindow=-1;
            boolean outputThisShingle=true;
            //this where the token n-gram is built
            for (int gramNum = 1;
                 iter.hasNext() && builtGramSize < gramSize.getValue();
                 ++gramNum) {

                nextToken = iter.next();
                idxInWindow++;
                if(idxInWindow==0)
                    outputThisShingle = checkToken(nextToken);

                if(outputThisShingle && sentenceBoundaryAware)
                    outputThisShingle = checkSentenceBoundary(idxInWindow);
                if(outputThisShingle&&idxInWindow==gramSize.getValue()-1)
                    outputThisShingle=checkToken(nextToken);

                if (builtGramSize < gramNum) {
                    if (builtGramSize > 0) {
                        gramBuilder.append(tokenSeparator);
                    }
                    gramBuilder.append(nextToken.termAtt.buffer(), 0,
                            nextToken.termAtt.length());
                    ++builtGramSize;
                }
                if (isAllFiller && nextToken.isFiller) {
                    if (gramNum == gramSize.getValue()) {
                        gramSize.advance();
                    }
                } else {
                    isAllFiller = false;
                }
            }
            if (!isAllFiller && builtGramSize == gramSize.getValue()) {
                if(gramBuilder.length()>maxCharLength || gramBuilder.length()<minCharLength)
                    outputThisShingle=false;

                if(outputThisShingle) {
                    inputWindow.getFirst().attSource.copyTo(this);
                    posIncrAtt.setPositionIncrement(isOutputHere ? 0 : 1);
                    termAtt.setEmpty().append(gramBuilder);
                    if (gramSize.getValue() > 1) {
                        typeAtt.setType(tokenType);
                        noShingleOutput = false;
                    }
                    offsetAtt.setOffset(offsetAtt.startOffset(), nextToken.offsetAtt.endOffset());
                    if (sentenceBoundaryAware && sentenceBoundaryInWindow != -1)
                        posLenAtt.setPositionLength(sentenceBoundaryInWindow + 1);
                    else
                        posIncrAtt.setPositionIncrement(builtGramSize);
                    isOutputHere = true;
                    gramSize.advance();
                    tokenAvailable = true;
                    //System.out.println(new String(offsetAtt.startOffset()+"-"+offsetAtt.endOffset()+","+gramBuilder.toString()));
                }else{
                    clearAttributes();
                    //termAtt.setEmpty();
                    //inputWindow.getFirst().attSource.copyTo(this);
                    gramSize.advance();
                    isOutputHere=true;
                    tokenAvailable=true;
                }
            }
        }
        return tokenAvailable;
    }

    private boolean checkToken(InputWindowToken nextToken) {
        if((removeLeadingStopwords||removeTrailingStopwords) && stopWords!=null){
            String token=new String(nextToken.termAtt.buffer(), 0,nextToken.termAtt.length());
            if(stopWordsIgnoreCase)
                token=token.toLowerCase();
            if(stopWords.contains(token))
                return false;
        }
        if(removeLeadingSymbolicTokens||removeTrailingStopwords){
            String token = new String(nextToken.termAtt.buffer(), 0,nextToken.termAtt.length());
            token = token.replaceAll("\\p{Punct}+","");
            if (token.length()==0)
                return false;
        }
        return true;
    }

    private boolean checkSentenceBoundary(int idxInWindow) {
        if(sentenceBoundaryInWindow!=-1&&idxInWindow>sentenceBoundaryInWindow){
            return false;
        }
        return true;
    }


    //does current window of tokens contain a sentence boundary? if so return its index in the window
    private int findSentenceBoundary() {
        int i = -1;
        if (sentenceBoundaryAware) {
            Iterator<InputWindowToken> it = inputWindow.iterator();
            while (it.hasNext()) {
                i++;

                InputWindowToken tok = it.next();
                if (tok.sentenceBoundaryFlag.getFlags() == 1) {
                    return i;
                }

            }
        }
        return -1;
    }

    private boolean exhausted;

    /**
     * <p>Get the next token from the input stream.
     * <p>If the next token has <code>positionIncrement &gt; 1</code>,
     * <code>positionIncrement - 1</code> {@link #fillerToken}s are
     * inserted first.
     *
     * @param target Where to put the new token; if null, a new instance is created.
     * @return On success, the populated token; null otherwise
     * @throws IOException if the input stream has a problem
     */
    private InputWindowToken getNextToken(InputWindowToken target)
            throws IOException {
        InputWindowToken newTarget = target;
        if (numFillerTokensToInsert > 0) {
            if (null == target) {
                newTarget = new InputWindowToken(nextInputStreamToken.cloneAttributes());
            } else {
                nextInputStreamToken.copyTo(target.attSource);
            }
            // A filler token occupies no space
            newTarget.offsetAtt.setOffset(newTarget.offsetAtt.startOffset(),
                    newTarget.offsetAtt.startOffset());
            newTarget.termAtt.copyBuffer(fillerToken, 0, fillerToken.length);
            newTarget.isFiller = true;
            --numFillerTokensToInsert;
        } else if (isNextInputStreamToken) {
            if (null == target) {
                newTarget = new InputWindowToken(nextInputStreamToken.cloneAttributes());
            } else {
                nextInputStreamToken.copyTo(target.attSource);
            }
            isNextInputStreamToken = false;
            newTarget.isFiller = false;
        } else if (!exhausted) {
            if (input.incrementToken()) {
                if (null == target) {
                    newTarget = new InputWindowToken(cloneAttributes());
                } else {
                    this.copyTo(target.attSource);
                }
                if (posIncrAtt.getPositionIncrement() > 1) {
                    // Each output shingle must contain at least one input token,
                    // so no more than (maxShingleSize - 1) filler tokens will be inserted.
                    numFillerTokensToInsert = Math.min(posIncrAtt.getPositionIncrement() - 1, maxShingleSize - 1);
                    // Save the current token as the next input stream token
                    if (null == nextInputStreamToken) {
                        nextInputStreamToken = cloneAttributes();
                    } else {
                        this.copyTo(nextInputStreamToken);
                    }
                    isNextInputStreamToken = true;
                    // A filler token occupies no space
                    newTarget.offsetAtt.setOffset(offsetAtt.startOffset(), offsetAtt.startOffset());
                    newTarget.termAtt.copyBuffer(fillerToken, 0, fillerToken.length);
                    newTarget.isFiller = true;
                    --numFillerTokensToInsert;
                } else {
                    newTarget.isFiller = false;
                }
            } else {
                exhausted = true;
                input.end();
                endState = captureState();
                numFillerTokensToInsert = Math.min(posIncrAtt.getPositionIncrement(), maxShingleSize - 1);
                if (numFillerTokensToInsert > 0) {
                    nextInputStreamToken = new AttributeSource(getAttributeFactory());
                    nextInputStreamToken.addAttribute(CharTermAttribute.class);
                    OffsetAttribute newOffsetAtt = nextInputStreamToken.addAttribute(OffsetAttribute.class);
                    newOffsetAtt.setOffset(offsetAtt.endOffset(), offsetAtt.endOffset());
                    // Recurse/loop just once:
                    return getNextToken(target);
                } else {
                    newTarget = null;
                }
            }
        } else {
            newTarget = null;
        }
        return newTarget;
    }

    @Override
    public void end() throws IOException {
        if (!exhausted) {
            super.end();
        } else {
            restoreState(endState);
        }
    }

    /**
     * <p>Fills {@link #inputWindow} with input stream tokens, if available,
     * shifting to the right if the window was previously full.
     * <p>Resets {@link #gramSize} to its minimum value.
     *
     * @throws IOException if there's a problem getting the next token
     */
    private boolean shiftInputWindow() throws IOException {
        InputWindowToken firstToken = null;
        if (inputWindow.size() > 0) {
            firstToken = inputWindow.removeFirst();
        }
        while (inputWindow.size() < maxShingleSize) {
            if (null != firstToken) {  // recycle the firstToken, if available
                if (null != getNextToken(firstToken)) {
                    inputWindow.add(firstToken); // the firstToken becomes the last
                    firstToken = null;
                } else {
                    break; // end of input stream
                }
            } else {
                InputWindowToken nextToken = getNextToken(null);
                if (null != nextToken) {
                    inputWindow.add(nextToken);
                } else {
                    break; // end of input stream
                }
            }
        }
        if (outputUnigramsIfNoShingles && noShingleOutput
                && gramSize.minValue > 1 && inputWindow.size() < minShingleSize) {
            gramSize.minValue = 1;
        }
        gramSize.reset();
        isOutputHere = false;

        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        gramSize.reset();
        inputWindow.clear();
        nextInputStreamToken = null;
        isNextInputStreamToken = false;
        numFillerTokensToInsert = 0;
        isOutputHere = false;
        noShingleOutput = true;
        exhausted = false;
        endState = null;
        if (outputUnigramsIfNoShingles && !outputUnigrams) {
            // Fix up gramSize if minValue was reset for outputUnigramsIfNoShingles
            gramSize.minValue = minShingleSize;
        }
    }


    /**
     * <p>An instance of this class is used to maintain the number of input
     * stream tokens that will be used to compose the next unigram or shingle:
     * {@link #gramSize}.
     * <p><code>gramSize</code> will take on values from the circular sequence
     * <b>{ [ 1, ] {@link #minShingleSize} [ , ... , {@link #maxShingleSize} ] }</b>.
     * <p>1 is included in the circular sequence only if
     * {@link #outputUnigrams} = true.
     */
    private class CircularSequence {
        private int value;
        private int previousValue;
        private int minValue;

        public CircularSequence() {
            minValue = outputUnigrams ? 1 : minShingleSize;
            reset();
        }

        /**
         * @return the current value.
         * @see #advance()
         */
        public int getValue() {
            return value;
        }

        /**
         * <p>Increments this circular number's value to the next member in the
         * circular sequence
         * <code>gramSize</code> will take on values from the circular sequence
         * <b>{ [ 1, ] {@link #minShingleSize} [ , ... , {@link #maxShingleSize} ] }</b>.
         * <p>1 is included in the circular sequence only if
         * {@link #outputUnigrams} = true.
         */
        public void advance() {
            previousValue = value;
            if (value == 1) {
                value = minShingleSize;
            } else if (value == maxShingleSize) {
                reset();
            } else {
                ++value;
            }
        }

        /**
         * <p>Sets this circular number's value to the first member of the
         * circular sequence
         * <p><code>gramSize</code> will take on values from the circular sequence
         * <b>{ [ 1, ] {@link #minShingleSize} [ , ... , {@link #maxShingleSize} ] }</b>.
         * <p>1 is included in the circular sequence only if
         * {@link #outputUnigrams} = true.
         */
        public void reset() {
            previousValue = value = minValue;
        }

        /**
         * <p>Returns true if the current value is the first member of the circular
         * sequence.
         * <p>If {@link #outputUnigrams} = true, the first member of the circular
         * sequence will be 1; otherwise, it will be {@link #minShingleSize}.
         *
         * @return true if the current value is the first member of the circular
         * sequence; false otherwise
         */
        public boolean atMinValue() {
            return value == minValue;
        }

        /**
         * @return the value this instance had before the last advance() call
         */
        public int getPreviousValue() {
            return previousValue;
        }
    }

    private class InputWindowToken {
        final AttributeSource attSource;
        final CharTermAttribute termAtt;
        final OffsetAttribute offsetAtt;
        final FlagsAttribute sentenceBoundaryFlag;
        boolean isFiller = false;

        public InputWindowToken(AttributeSource attSource) {
            this.attSource = attSource;
            this.termAtt = attSource.getAttribute(CharTermAttribute.class);
            this.offsetAtt = attSource.getAttribute(OffsetAttribute.class);
            this.sentenceBoundaryFlag = attSource.getAttribute(FlagsAttribute.class);
        }
    }
}