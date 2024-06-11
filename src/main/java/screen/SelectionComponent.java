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

import java.io.IOException;
import com.github.kwhat.jnativehook.GlobalScreen;
//import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.*;

/**
 *
 * @author kaiyu
 * 
 * SelectionComponent - widget for selecting a choice from a list
 */
public class SelectionComponent extends Component implements NativeKeyListener {
    private final String[] choices;
    private int selectedIndex;
    // Set to true when the user has selected the choice
    private boolean finalChoice;
    
    /**
     * The order in choices is used in the selection field, and the chosen
     * selection's index is returned
     * 
     * @param startX
     * @param startY
     * @param sizeX
     * @param sizeY
     * @param layer
     * @param choices 
     */
    public SelectionComponent(int startX, int startY, int sizeX, int sizeY, int layer, String[] choices) throws NullPointerException {
        super(startX, startY, sizeX, sizeY, layer);
        
        for (String choice : choices) {
            if (choice == null) {
                throw new NullPointerException();
            }
        }
        
        this.choices = choices;
        this.selectedIndex = 0;
        this.finalChoice = false;
        
        addListener();
    }
    
    /**
     * The constructor which most are going to use. Automatically sets size.
     * 
     * sizeX is set with the length of the longest choice String
     * 
     * @param startX
     * @param startY
     * @param layer
     * @param choices
     */
    public SelectionComponent(int startX, int startY, int layer, String[] choices) throws NullPointerException {        
        this(startX, startY, getLongestString(choices), choices.length, layer, choices);
    }
    
    private void addListener() {
        GlobalScreen.addNativeKeyListener(this);
    }
    
    private static int getLongestString(String[] a) throws NullPointerException {
        int maxLength = a[0].length();
        
        for (String choice : a) {
            if (choice == null) {
                throw new NullPointerException();
            }
            if (choice.length() > maxLength) {
                maxLength = choice.length();
            }
        }
        
        return maxLength;
    }
        
    @Override
    public DisplayCharacter[][] render() {
        int sizeX = super.getSizeX();
        int sizeY = super.getSizeY();
        DisplayCharacter[][] result = new DisplayCharacter[sizeY][sizeX];
        super.clearAttr();
        
        for (int i = 0; i < choices.length; i++) {
            int choiceLength = choices[i].length();
            if (i == selectedIndex) {
                super.addDisplayAttr(SGR.UNDERLINE, i, 0, choiceLength);
            }
            for (int c = 0; c < choiceLength; c++) {
                result[i][c] = new DisplayCharacter(
                    choices[i].charAt(c),
                    super.getDisplayAttr(i, c)
                );
            }
        }
        
        return result;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Only do something if finalChoice is not yet true
        if (!finalChoice) {
            switch (e.getKeyCode()) {
                // Because the lower selections are the higher indices, "up is
                // down"
                case NativeKeyEvent.VC_DOWN:
                    // Wrap selection if it goes over
                    selectedIndex = (selectedIndex + 1) % choices.length;
                    break;
                case NativeKeyEvent.VC_UP:
                    if (selectedIndex - 1 < 0)
                        selectedIndex = choices.length - 1;
                    else
                        selectedIndex -= 1;
                    break;
                case NativeKeyEvent.VC_ENTER:
                    // User has chosen. Assign finalChoice to true and remove the 
                    // component from the screen component list
                    this.finalChoice = true;
                    Screen.removeComponent(this);
                    GlobalScreen.removeNativeKeyListener(this);
                // Ignore any other key presses
                default: break;
            }

            // Can't throw an exception, so exit using try-catch
            try {
                Screen.refresh();
            } catch(IOException exc) {
                System.out.print(exc.getMessage());
                System.exit(1);
            }
        }
    }
    
    /**
     * A hacky way of getting the chosen index
     * Waits for finalChoice to be true and returns selectedIndex
     * 
     * @return the selected index
     * @throws java.lang.InterruptedException
     */
    public int getChoice() throws InterruptedException {
        // Wait in intervals until there's a final choice
        while (!finalChoice) {
            Thread.sleep(100);
        }
        return selectedIndex;
    }
}
