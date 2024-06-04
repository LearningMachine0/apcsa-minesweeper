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
 * Component to display a Char array
 * 
 * @author kaiyu
 */
public class CharArrayComponent extends Component {
    char[][] array;
    
    /**
     * 
     * @param startX
     * @param startY
     * @param sizeX
     * @param sizeY
     * @param layer
     * @param array 
     */
    public CharArrayComponent(int startX, int startY, int sizeX, int sizeY, int layer, char[][] array) {
        super(startX, startY, sizeX, sizeY, layer);
        this.array = array;
    }
    
    /**
     * Sets the array for the component. The size of the component will
     * compensate for the array size
     * 
     * @param array 
     */
    public void setArray(char[][] array) {
        this.array = array;
        super.setSizeY(array.length);
        super.setSizeX(array[0].length);
    }
    
    @Override
    public DisplayCharacter[][] render() {
        int sizeX = super.getSizeX();
        int sizeY = super.getSizeY();
        DisplayCharacter[][] result = new DisplayCharacter[sizeY][sizeX];
        
        for (int r = 0; r < sizeX; r++) {
            for (int c = 0; c < sizeY; c++) {
                result[r][c] = new DisplayCharacter(
                        array[r][c],
                        super.getDisplayAttr(r, c)
                );
            }
        }
        
        return result;
    }
}
