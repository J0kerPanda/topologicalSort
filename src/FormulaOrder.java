import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.IntPredicate;

class Position
{
    private String allText;
    private int index;
    private int row;
    private int column;

    private Position( String allText, int index, int row, int column )
    {
        this.allText = allText;
        this.index = index;
        this.row = row;
        this.column = column;
    }

    public Position(String allText)
    {
        this( allText, 0, 1, 1 );
    }

    public int getChar()
    {
        return this.index < this.allText.length() ? this.allText.codePointAt(this.index) : -1;
    }

    public Position next()
    {
        switch( this.getChar() )
        {
            case -1:
                return this;
            case '\n':
                return new Position( this.allText, this.index + 1, this.row + 1, 1 );
            default:
                return new Position( this.allText, this.index + 1, this.row, this.column + 1 );
        }
    }

    public boolean satisfies( IntPredicate predicate )
    {
        return predicate.test( this.getChar() );
    }

    public Position nextWhile( IntPredicate predicate )
    {
        Position currentPosition = this;

        while( currentPosition.satisfies( predicate ) )
        {
            currentPosition = currentPosition.next();
        }

        return currentPosition;
    }

    public int getIndex()
    {
        return index;
    }

    public String getSubString( int length )
    {
        String subString = "";
        Position currentPos = this;

        for ( int i = 0; i < length; ++i )
        {
            subString = subString + ( char )currentPos.getChar();
            currentPos = currentPos.next();
        }

        return subString;
    }

    public String toString()
    {
        return "row: " + this.row + "; column: " + this.column;
    }
}

class SyntaxError extends Exception
{
    SyntaxError( Position position, String errorMessage )
    {
        super("Error at " + position + ".\n" + errorMessage);
    }
}

class SemanticError extends Exception
{
    SemanticError( String errorMessage )
    {
        super( errorMessage );
    }
}

enum TokenTag
{
    COMMA,
    END_OF_TEXT,
    EQUAL_SIGN,
    IDENT,
    LEFT_BRACKET,
    MUL_SIGN,
    NUMBER,
    RIGHT_BRACKET,
    SEMICOLON,
    SUM_SIGN;

    @Override
    public String toString()
    {
        switch ( this )
        {
            case COMMA:
                return ",";
            case END_OF_TEXT:
                return "END OF TEXT";
            case EQUAL_SIGN:
                return "=";
            case IDENT:
                return "IDENT";
            case LEFT_BRACKET:
                return "(";
            case MUL_SIGN:
                return "* or /";
            case NUMBER:
                return "NUMBER";
            case RIGHT_BRACKET:
                return ")";
            case SEMICOLON:
                return ";";
            case SUM_SIGN:
                return "+ or -";
        }

        return "SOMETHING UNUSUAL";
    }
}
/*
<All-formulasDeclarations> ::= <Formula-names> = <Formulas> NEXTLINE <Rest-formulasDeclarations>
<Formula-names> ::= IDENT <Formula-names-sub> //Names must be unique
<Formula-names-sub> ::= , <Formula-names> | E
<Rest-formulasDeclarations> ::= <All-formulasDeclarations> | E

<Formulas> ::= <Sum> <Next-formula>
<Next-formula> ::= , <Sum> | E //count of formulasDeclarations and formulasDeclarations-names must match

<Sum> ::= <Mul> <Sum-sub>
<Sum-sub> ::= + <Sum> | - <Sum> | E

<Mul> ::= <Var> <Mul-sub>
<Var> ::= NUMBER | IDENT | ( <Sum> ) | - <Var> //all idents must be defined in functions
<Mul-sub> ::= * <Mul> | / <Mul> | E
*/

class Token
{
    private TokenTag tag;
    private Position start;
    private Position follow;

    public Token( String allText ) throws SyntaxError
    {
        this( new Position( allText ) );
    }

    private Token( Position startPosition ) throws SyntaxError
    {
        this.start = startPosition.nextWhile( Character::isWhitespace );
        this.follow = this.start.next();
        switch( this.start.getChar() )
        {
            case -1:
                this.tag = TokenTag.END_OF_TEXT;
                break;

            case ';':
                this.tag = TokenTag.SEMICOLON;
                break;

            case '(':
                this.tag = TokenTag.LEFT_BRACKET;
                break;

            case ')':
                this.tag = TokenTag.RIGHT_BRACKET;
                break;

            case '=':
                this.tag = TokenTag.EQUAL_SIGN;
                break;

            case '+':
                this.tag = TokenTag.SUM_SIGN;
                break;

            case '-':
                this.tag = TokenTag.SUM_SIGN;
                break;

            case '*':
                this.tag = TokenTag.MUL_SIGN;
                break;

            case '/':
                this.tag = TokenTag.MUL_SIGN;
                break;

            case ',':
                this.tag = TokenTag.COMMA;
                break;

            default:
                if( this.start.satisfies( Character::isLetter ) )
                {
                    this.follow = this.follow.nextWhile( Character::isLetterOrDigit );
                    this.tag = TokenTag.IDENT;
                }
                else if( this.start.satisfies( Character::isDigit ) )
                {
                    this.follow = this.follow.nextWhile( Character::isDigit );
                    if( this.follow.satisfies( Character::isLetter ) )
                    {
                        this.throwSyntaxException( "letters after series of digits" );
                    }

                    this.tag = TokenTag.NUMBER;
                }
                else
                {
                    this.throwSyntaxException( "unknown character" );
                }
        }
    }

    public TokenTag getTag()
    {
        return tag;
    }

    public Position getStart()
    {
        return start;
    }

    public void throwSyntaxException(String errorMessage ) throws SyntaxError
    {
        throw new SyntaxError( this.follow, errorMessage );
    }

    public boolean matches( TokenTag ... tags )
    {
        return Arrays.stream( tags ).anyMatch(t -> t == this.tag );
    }

    public Token next() throws SyntaxError
    {
        return new Token( this.follow );
    }

    @Override
    public String toString()
    {
        return start.getSubString( follow.getIndex() - start.getIndex() );
    }
}

class Parser
{
    private Token currentToken;
    private ArrayList< String > definedNames;
    private ArrayList< String > calledNames;

    private ArrayList< Vertex > currentDeclarations;
    private ArrayList< ArrayList< Vertex > > currentCalls;

    private HashMap< String, Vertex > subFormulas; //Idents

    private ArrayList< Vertex > formulas;

    public Parser( String allText ) throws SyntaxError, SemanticError
    {
        definedNames = new ArrayList<>();
        calledNames = new ArrayList<>();

        subFormulas = new HashMap<>();

        formulas = new ArrayList<>();

        parse( allText );
        makeFormulas();
    }

    private void makeFormulas()
    {
        for ( Vertex currentFormula : formulas )
        {
            for ( Vertex relatedFormula : formulas )
            {
                if ( currentFormula.equals( relatedFormula ) )
                {
                    continue;
                }

                for ( Vertex declaredRelatedVertex : relatedFormula.getDeclaredVertices() )
                {
                    if ( currentFormula.getCalledVertices().contains( declaredRelatedVertex ) )
                    {
                        currentFormula.addRelatedVertex( relatedFormula );
                        break;
                    }
                }

            }
        }
    }

    public ArrayList<Vertex> getFormulas()
    {
        return formulas;
    }

    private void expect(TokenTag tag ) throws SyntaxError
    {
        if ( !currentToken.matches( tag ) )
        {
            currentToken.throwSyntaxException( "Expected " + tag.toString() + ". Got " + currentToken.getTag().toString() );
        }

        currentToken = currentToken.next();
    }

    private void parse( String allText) throws SyntaxError, SemanticError
    {
        allText = allText + "\n";

        String currentLine = allText.substring( 0, allText.indexOf( "\n" ) + 1 );

        while ( !allText.equals( "" ) )
        {
            currentToken = new Token( currentLine );
            parseAllFormulas();

            allText = allText.substring( allText.indexOf( "\n" ) + 1 );
            currentLine = allText.substring( 0, allText.indexOf( "\n" ) + 1 );
        }

        calledNames.removeAll( definedNames );

        if ( calledNames.size() != 0 )
        {
            throw new SyntaxError( currentToken.getStart(), "Some of the formulasDeclarations remained undefined" );
        }


        expect( TokenTag.END_OF_TEXT );
    }

    private void parseAllFormulas() throws SyntaxError, SemanticError
    {
        Position start = currentToken.getStart();

        currentDeclarations = new ArrayList<>();
        currentCalls = new ArrayList<>();

        parseFormulaNames();

        expect( TokenTag.EQUAL_SIGN );
        parseFormulas();
        //Possible source of troubles (Not expecting NEXTLINE)

        if ( currentDeclarations.size() != currentCalls.size() )
        {
            throw new SyntaxError( currentToken.getStart(), "Bad declaration" );
        }

        for ( int currentDeclaration = 0; currentDeclaration < currentDeclarations.size(); ++ currentDeclaration )
        {
            for ( Vertex relatedVertex : currentCalls.get( currentDeclaration ) )
            {
                currentDeclarations.get( currentDeclaration ).addRelatedVertex( relatedVertex );

                if ( currentDeclarations.contains( relatedVertex ) )
                {
                    throw new SemanticError( "Cycle declaration" );
                }
            }
        }

        Position finish = currentToken.getStart();

        String currentFormulaText = start.getSubString( finish.getIndex() - start.getIndex() ).replaceAll( "\n", "" );

        Vertex newFormula = new Vertex( currentFormulaText );
        newFormula.setDeclaredVertices( currentDeclarations );

        for ( ArrayList< Vertex > call : currentCalls )
        {
            newFormula.addCalledVertices( call );
        }

        formulas.add( newFormula );

        expect( TokenTag.END_OF_TEXT );
    }

    private void parseFormulaNames() throws SyntaxError, SemanticError
    {
        if ( currentToken.matches( TokenTag.IDENT ) )
        {
            String currentName = currentToken.toString();

            if ( !subFormulas.keySet().contains( currentName ) )
            {
                subFormulas.put( currentName, new Vertex( currentName ) );
            }
            else
            {
                if ( definedNames.contains( currentName ) )
                {
                    throw new SyntaxError( currentToken.getStart(), "Variable was already defined" );
                }
            }

            definedNames.add( currentName );
            currentDeclarations.add( subFormulas.get( currentName ) );

            expect( TokenTag.IDENT );

            parseFormulaNamesSub();
        }
        else
        {
            expect( TokenTag.IDENT );
        }
    }

    private void parseFormulaNamesSub() throws SyntaxError, SemanticError
    {
        if ( currentToken.matches( TokenTag.COMMA ) )
        {
            expect( TokenTag.COMMA );
            parseFormulaNames();
        }
    }

    private void parseFormulas() throws SyntaxError
    {
        currentCalls.add( new ArrayList<>() );

        parseSum();

        parseNextFormula();
    }

    private void parseNextFormula() throws SyntaxError
    {
        if ( currentToken.matches( TokenTag.COMMA ) )
        {
            expect( TokenTag.COMMA );
            parseFormulas();
        }
    }

    private void parseSum() throws SyntaxError
    {
        parseMul();
        parseSumSub();
    }

    private void parseSumSub() throws SyntaxError
    {
        if ( currentToken.matches( TokenTag.SUM_SIGN ) )
        {
            expect( TokenTag.SUM_SIGN );
            parseSum();
        }
    }

    private void parseMul() throws SyntaxError
    {
        parseVar();
        parseMulSub();
    }

    private void parseVar() throws SyntaxError
    {
        if ( currentToken.matches( TokenTag.NUMBER ) )
        {
            expect( TokenTag.NUMBER );
            return;
        }
        else if ( currentToken.matches( TokenTag.IDENT ) )
        {
            String currentName = currentToken.toString();

            if ( !subFormulas.keySet().contains( currentName ) )
            {
                subFormulas.put( currentName, new Vertex( currentName ) );
            }

            if ( !currentCalls.get( currentCalls.size() - 1 ).contains( subFormulas.get( currentName ) ) )
            {
                currentCalls.get( currentCalls.size() - 1 ).add( subFormulas.get( currentName ) );
            }

            if ( !calledNames.contains( currentName ) )
            {
                calledNames.add( currentName );
            }

            expect( TokenTag.IDENT );
            return;
        }
        else if ( currentToken.matches( TokenTag.LEFT_BRACKET ) )
        {
            expect( TokenTag.LEFT_BRACKET );
            parseSum();
            expect( TokenTag.RIGHT_BRACKET );
            return;
        }
        else if ( currentToken.matches( TokenTag.SUM_SIGN ) )
        {
            expect( TokenTag.SUM_SIGN );
            parseVar();
            return;
        }

        throw new SyntaxError( currentToken.getStart(), "UNKNOWN TOKEN" );
    }

    private void parseMulSub() throws SyntaxError
    {
        if ( currentToken.matches( TokenTag.MUL_SIGN ) )
        {
            expect( TokenTag.MUL_SIGN );
            parseMul();
        }
    }
}

class Vertex implements Comparable< Vertex >
{
    enum COLOR
    {
        BLACK,
        GREY,
        WHITE
    }

    private String name;
    private ArrayList< Vertex > relatedVertices;
    private ArrayList< Vertex > declaredVertices;
    private ArrayList< Vertex > calledVertices;
    private COLOR color;
    private int timeExit;

    public Vertex( String name )
    {
        this.name = name;
        this.relatedVertices = new ArrayList<>();
        this.declaredVertices = new ArrayList<>();
        this.calledVertices = new ArrayList<>();
        this.color = COLOR.WHITE;
        this.timeExit = 0;
    }

    public String getName()
    {
        return name;
    }

    void addRelatedVertex(Vertex vertex )
    {
        this.relatedVertices.add( vertex );
    }

    public ArrayList<Vertex> getRelatedVertices()
    {
        return relatedVertices;
    }

    public void setDeclaredVertices(ArrayList<Vertex> declaredVertices)
    {
        this.declaredVertices = declaredVertices;
    }

    public ArrayList<Vertex> getDeclaredVertices()
    {
        return declaredVertices;
    }

    public void addCalledVertices(ArrayList<Vertex> calledVertices)
    {
        this.calledVertices.addAll( calledVertices );
    }

    public ArrayList<Vertex> getCalledVertices()
    {
        return calledVertices;
    }

    public void setColor(COLOR color)
    {
        this.color = color;
    }

    public COLOR getColor()
    {
        return color;
    }

    public void setTimeExit( int timeExit )
    {
        this.timeExit = timeExit;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public int compareTo( Vertex o )
    {
        return this.timeExit - o.timeExit;
    }
}

public class FormulaOrder
{
    private static int time;

    public static void main( String[] args ) throws SyntaxError, SemanticError
    {
        String allText = new Scanner( System.in ).useDelimiter( "\\Z" ).next();

        try
        {
            Parser parser = new Parser( allText );

            ArrayList< Vertex > formulas = parser.getFormulas();

            topologicalSort( formulas );

            formulas.sort( Vertex::compareTo );

            for ( Vertex formula : formulas )
            {
                System.out.println( formula.getName() );
            }
        }
        catch ( SyntaxError e )
        {
            System.out.println( "syntax error" );
        }
        catch ( SemanticError e )
        {
            System.out.println( "cycle" );
        }
    }

    private static void topologicalSort( ArrayList< Vertex > vertices ) throws SemanticError
    {
        time = 1;

        for( Vertex vertex : vertices )
        {
            if ( vertex.getColor() == Vertex.COLOR.WHITE )
            {
                visitVertex( vertex );
            }
        }
    }

    private static void visitVertex( Vertex vertex ) throws SemanticError
    {
        vertex.setColor( Vertex.COLOR.GREY );

        for ( Vertex relatedVertex : vertex.getRelatedVertices() )
        {
            if ( relatedVertex.getColor() == Vertex.COLOR.GREY )
            {
                throw new SemanticError( "Cycle detected" );
            }

            if ( relatedVertex.getColor() == Vertex.COLOR.WHITE )
            {
                visitVertex( relatedVertex );
            }
        }

        vertex.setTimeExit( time );
        vertex.setColor( Vertex.COLOR.BLACK );

        ++time;
    }
}


