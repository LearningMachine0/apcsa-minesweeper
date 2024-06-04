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
 *
 * @author kaiyu
 * 
 * TextComponent - used for displaying text
 * Text is character wrapped and will throw an exception if the entire text
 * can't fit into the set area
 */
public class TextComponent extends Component {
    private String text;
    
    public TextComponent(int startX, int startY, int sizeX, int sizeY, int layer, String text) throws IllegalArgumentException {
        super(startX, startY, sizeX, sizeY, layer);
        
        if (text.length() > (super.getSizeX() * super.getSizeY()))
            throw new IllegalArgumentException("New text can't fit into area");

        this.text = text;
    }
    
    public void setText(String newText) throws IllegalArgumentException {
        if (newText.length() > (super.getSizeX() * super.getSizeY()))
            throw new IllegalArgumentException(
                    String.format(
                            "New text (\"%s\", l%d c%d) can't fit into area size %d by %d",
                            newText,
                            super.getStartY(),
                            super.getStartX(),
                            super.getSizeX(),
                            super.getSizeY()
                    )
            );
        
        this.text = newText;
    }
    
    @Override
    public DisplayCharacter[][] render() {
        int sizeX = super.getSizeX();
        int sizeY = super.getSizeY();
        DisplayCharacter[][] result = new DisplayCharacter[sizeY][sizeX];
        int textIndex = 0;
        
        for (int r = 0; r < sizeY; r++) {
            for (int c = 0; c < sizeX; c++) {
                if (textIndex >= text.length()) break;
                
                result[r][c] = new DisplayCharacter(
                        text.charAt(textIndex),
                        super.getDisplayAttr(r, c)
                );
                textIndex++;
            }
        }
        
        return result;
    }
}
