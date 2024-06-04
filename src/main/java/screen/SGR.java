/*
 * The MIT License
 *
 * Copyright 2024 kaiyu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package screen;

/**
 * Enum representing the display attributes used in the game (italic, underline,
 * blink, colours, etc). The attribute n stores the control sequence number for 
 * that attribute. Attributes which turn off a display attribute is prefixed
 * with NO
 * 
 * @author kaiyu
 */
public enum SGR {
    RESET(0),
    BOLD(1),
    ITALIC(3),
    UNDERLINE(4),
    BLINK(5),
    NOBOLD(22),
    NOITALIC(23),
    NOUNDERLINE(24),
    NOBLINK(25),
    FGBLACK(30),
    FGRED(31),
    FGGREEN(32),
    FGBLUE(34),
    FGCYAN(36),
    FGGREY(90),
    FGBRIGHTRED(91),
    FGBRIGHTGREEN(92),
    FGBRIGHTBLUE(94),
    BGWHITE(107);
    
    final int n;
    
    private SGR(int n) {
        this.n = n;
    }
    
    @Override
    public String toString() {
        return "\033[" + n + "m";
    }
    
    public byte[] getBytes() {
        return toString().getBytes();
    }

    public boolean equals(SGR other) {
        return n == other.n;
    }
    
    /**
     * Returns the end attribute for a display attribute. If no appropriate end
     * attribute is found, return the attribute given in the argument.
     * 
     * @param a
     * @return the appropriate end attribute
     */
    public static SGR getEndAttribute(SGR a) {
        switch (a) {
            case BOLD: return NOBOLD;
            case ITALIC: return NOITALIC;
            case UNDERLINE: return NOUNDERLINE;
            case BLINK: return NOBLINK;
            default: return a;
        }
    }
}
