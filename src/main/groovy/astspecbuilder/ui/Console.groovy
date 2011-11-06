/*
 * Copyright 2011 Nagai Masato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package astspecbuilder.ui

import groovy.ui.HistoryRecord

import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

import astspecbuilder.AstSpecBuilder;

/**
 * <p>This class provides the user interface of {@link AstSpecBuilder}.</p>
 * 
 * @author Nagai Masato
 *
 */
class Console extends groovy.ui.Console {

    static void main(args) {
        new Console().run()
    }
    
    static def frameConsoleDelegates = [
            rootContainerDelegate:{
                frame(
                    title: 'AstSpecBuilder',
                    iconImage: imageIcon("/groovy/ui/ConsoleIcon.png").image,
                    defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE,
                ) {
                    try {
                        current.locationByPlatform = true
                    } catch (Exception e) {
                        current.location = [100, 100] // for 1.4 compatibility
                    }
                    containingWindows += current
                }
            },
            menuBarDelegate: {arg->
                current.JMenuBar = build(arg)}
        ];
    
    void run() {
        UIManager.lookAndFeel = UIManager.systemLookAndFeelClassName;
        super.run(frameConsoleDelegates)    
    }
    
    void runScript(EventObject eo) {
        def endLine = System.getProperty('line.separator')
        def record = new HistoryRecord(
            allText: inputArea.getText().replaceAll(endLine, '\n'),
            selectionStart: textSelectionStart, 
            selectionEnd: textSelectionEnd)
        addToHistory(record)
        pendingRecord = new HistoryRecord(
            allText: '', 
            selectionStart: 0, 
            selectionEnd: 0)

        runThread = Thread.start {
            try {
                SwingUtilities.invokeLater { showExecutingMessage() }
                def spec = new AstSpecBuilder().build(inputEditor.textEditor.text)
                SwingUtilities.invokeLater { finishNormal(endLine + spec) }
            } catch (Throwable t) {
                if(t instanceof StackOverflowError) {
                    stackOverFlowError = true
                    clearOutput()
                } 
                SwingUtilities.invokeLater { finishException(t, true) }
            } finally {
                runThread = null
                interruptAction.enabled = false
            } 
        }
	} 

}
