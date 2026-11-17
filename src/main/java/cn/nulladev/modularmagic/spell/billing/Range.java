package cn.nulladev.modularmagic.spell.billing;

import java.util.*;

/** Conservative pure-expression interval evaluator used for reservation, never sampling random values. */
public record Range(double lo,double hi) {
    public Range { if(!Double.isFinite(lo)||!Double.isFinite(hi)||lo>hi)throw new IllegalArgumentException("Unbounded expression"); }
    public static Range at(double n){return new Range(n,n);}
    public double magnitude(){return Math.max(Math.abs(lo),Math.abs(hi));}
    private static final java.util.regex.Pattern POSITIVE_RATIO=java.util.regex.Pattern.compile("\\(\\s*([A-Za-z_]\\w*)\\s*-\\s*min\\(\\s*([A-Za-z_]\\w*)\\s*,\\s*([A-Za-z_]\\w*)\\s*\\)\\s*\\)\\s*/\\s*\\(\\s*([A-Za-z_]\\w*)\\s*-\\s*([A-Za-z_]\\w*)\\s*\\)");
    public static Range eval(String expression,Map<String,Range> variables){
        // max(a-b,0)/(a-b) is bounded on its defined domain. Independent intervals lose this correlation.
        var vars=new HashMap<>(variables);var match=POSITIVE_RATIO.matcher(expression);var text=new StringBuffer();int index=0;
        while(match.find()){
            String a=match.group(1),first=match.group(2),second=match.group(3),left=match.group(4),right=match.group(5);
            String b=a.equals(first)?second:a.equals(second)?first:null;
            if(b==null||!(left.equals(a)&&right.equals(b)||left.equals(b)&&right.equals(a))){match.appendReplacement(text,java.util.regex.Matcher.quoteReplacement(match.group()));continue;}
            var x=vars.get(a);var y=vars.get(b);
            if(x==null||y==null||a.equals(b)||left.equals(right)){match.appendReplacement(text,java.util.regex.Matcher.quoteReplacement(match.group()));continue;}
            if(x.lo==x.hi&&y.lo==y.hi&&x.lo==y.lo)throw new IllegalArgumentException("Division by zero");
            double sign=left.equals(a)?1:-1;Range bound=x.hi<y.lo?at(0):x.lo>y.hi?at(sign):new Range(Math.min(0,sign),Math.max(0,sign));
            String key;do{key="__positive_ratio_"+index++;}while(vars.containsKey(key));vars.put(key,bound);match.appendReplacement(text,key);
        }
        match.appendTail(text);var p=new Parser(text.toString(),vars);var r=p.expr(0);p.space();if(p.i!=p.s.length())throw new IllegalArgumentException("Unsupported expression: "+expression);return r;
    }
    private static final class Parser {
        final String s;final Map<String,Range> vars;int i;
        Parser(String s,Map<String,Range> vars){this.s=s;this.vars=vars;}
        void space(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        Range expr(int min){Range a=atom();while(true){space();String op=operator();int prec=precedence(op);if(prec<min)break;i+=op.length();Range b=expr(prec+(op.equals("^")?0:1));a=apply(op,a,b);}return a;}
        String operator(){for(String op:List.of("<=",">=","==","!=","&&","||","+","-","*","/","%","^","<",">","&","|"))if(s.startsWith(op,i))return op;return "";}
        int precedence(String op){return switch(op){case "|","||"->0;case "&","&&"->1;case "==","!=","<",">","<=",">="->2;case "+","-"->3;case "*","/","%"->4;case "^"->5;default->-1;};}
        Range atom(){space();if(i>=s.length())throw new IllegalArgumentException("Missing operand");char c=s.charAt(i);
            if(c=='+'||c=='-'||c=='!'){i++;Range r=expr(5);return c=='-'?new Range(-r.hi,-r.lo):c=='!'?(r.lo==r.hi?at(r.lo==0?1:0):new Range(0,1)):r;}
            if(c=='('){i++;var r=expr(0);space();if(i>=s.length()||s.charAt(i++)!=')')throw new IllegalArgumentException("Missing parenthesis");return r;}
            if(Character.isDigit(c)||c=='.'){int start=i++;while(i<s.length()&&(Character.isDigit(s.charAt(i))||s.charAt(i)=='.'))i++;if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}return at(Double.parseDouble(s.substring(start,i)));}
            int start=i;while(i<s.length()&&(Character.isLetterOrDigit(s.charAt(i))||s.charAt(i)=='_'))i++;if(start==i)throw new IllegalArgumentException("Unknown token");String name=s.substring(start,i);space();
            if(i<s.length()&&s.charAt(i)=='('){i++;var args=new ArrayList<Range>();space();if(i<s.length()&&s.charAt(i)!=')'){do{args.add(expr(0));space();if(i<s.length()&&s.charAt(i)==','){i++;continue;}break;}while(true);}if(i>=s.length()||s.charAt(i++)!=')')throw new IllegalArgumentException("Bad function");return function(name,args);}
            if(name.equals("pi"))return at(Math.PI);if(name.equals("e"))return at(Math.E);var r=vars.get(name);if(r==null)throw new IllegalArgumentException("No bound for "+name);return r;
        }
        Range apply(String op,Range a,Range b){return switch(op){
            case "+"->new Range(a.lo+b.lo,a.hi+b.hi);case "-"->new Range(a.lo-b.hi,a.hi-b.lo);
            case "*"->ext(a.lo*b.lo,a.lo*b.hi,a.hi*b.lo,a.hi*b.hi);
            case "/"->{if(b.lo<=0&&b.hi>=0)throw new IllegalArgumentException("Division may be zero");yield apply("*",a,new Range(1/b.hi,1/b.lo));}
            case "%"->{if(b.lo<=0&&b.hi>=0)throw new IllegalArgumentException("Modulo may be zero");yield a.lo==a.hi&&b.lo==b.hi?at(a.lo%b.lo):new Range(-b.magnitude(),b.magnitude());}
            case "^"->power(a,b);default->{if(a.lo!=a.hi||b.lo!=b.hi)yield new Range(0,1);boolean result=switch(op){
                case "<"->a.lo<b.lo;case ">"->a.lo>b.lo;case "<="->a.lo<=b.lo;case ">="->a.lo>=b.lo;
                case "=="->a.lo==b.lo;case "!="->a.lo!=b.lo;case "&","&&"->a.lo!=0&&b.lo!=0;case "|","||"->a.lo!=0||b.lo!=0;
                default->throw new IllegalArgumentException("Unknown operator");};yield at(result?1:0);}};}
        Range function(String name,List<Range> a){Range x=a.getFirst();return switch(name){
            case "min"->new Range(Math.min(x.lo,a.get(1).lo),Math.min(x.hi,a.get(1).hi));case "max"->new Range(Math.max(x.lo,a.get(1).lo),Math.max(x.hi,a.get(1).hi));
            case "rand"->new Range(Math.min(x.lo,a.get(1).lo),Math.max(x.hi,a.get(1).hi));
            case "sin","cos","sinDegree","cosDegree"->{double angle=name.endsWith("Degree")?Math.toRadians(x.lo):x.lo;yield x.lo==x.hi?at(name.startsWith("sin")?Math.sin(angle):Math.cos(angle)):new Range(-1,1);}
            case "abs"->new Range(x.lo<=0&&x.hi>=0?0:Math.min(Math.abs(x.lo),Math.abs(x.hi)),x.magnitude());
            case "sqrt"->new Range(Math.sqrt(x.lo),Math.sqrt(x.hi));case "log","ln"->new Range(Math.log(x.lo),Math.log(x.hi));case "log10"->new Range(Math.log10(x.lo),Math.log10(x.hi));
            case "exp"->new Range(Math.exp(x.lo),Math.exp(x.hi));case "floor"->new Range(Math.floor(x.lo),Math.floor(x.hi));case "ceil"->new Range(Math.ceil(x.lo),Math.ceil(x.hi));
            case "pow"->power(x,a.get(1));default->throw new IllegalArgumentException("Unsupported cost function: "+name);};}
        Range power(Range a,Range b){if(b.lo!=b.hi)throw new IllegalArgumentException("Variable exponent");if(b.lo<0&&a.lo<=0&&a.hi>=0)throw new IllegalArgumentException("Negative power may divide by zero");if(a.lo<0&&b.lo!=Math.rint(b.lo))throw new IllegalArgumentException("Non-real power");double x=Math.pow(a.lo,b.lo),y=Math.pow(a.hi,b.lo);if(a.lo<=0&&a.hi>=0&&b.lo>0&&b.lo%2==0)return new Range(0,Math.max(x,y));return new Range(Math.min(x,y),Math.max(x,y));}
        Range ext(double... v){return new Range(Arrays.stream(v).min().orElseThrow(),Arrays.stream(v).max().orElseThrow());}
    }
}
