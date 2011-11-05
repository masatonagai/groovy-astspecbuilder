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

import groovy.swing.SwingBuilder
import groovy.ui.ConsoleTextEditor
import groovy.ui.text.TextEditor

import java.awt.event.ActionEvent

import javax.swing.JFrame
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.UIManager

import astspecbuilder.AstSpecBuilder

class AstSpecBrowser {
    
    JFrame mainFrame
    ConsoleTextEditor scriptEditor
    ConsoleTextEditor specView
    
    void run() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        def swing = new SwingBuilder()    
        swing.doLater { 
            mainFrame = frame(
                title: 'AST Spec Browser',
                defaultCloseOperation: JFrame.EXIT_ON_CLOSE
            ) {
                menuBar {
                    menu(text: 'File', mnemonic: 'F') {
                        menuItem {
                            action(name: 'New File',
                                closure: this.&clearScript,
                                mnemonic: 'N',
                                accelerator: shortcut('ctrl N'))
                        }
                        menuItem {
                            action(name: 'Exit',
                                closure: this.&exit)
                        }
                    }
                    menu(text: 'Build', mnemonic: 'B') {
                        menuItem { 
                            action(name: 'Run', 
                                closure: this.&buildSpec, 
                                mnemonic: 'R', 
                                accelerator: shortcut('ctrl R'))
                        }
                        menuItem { 
                            action(name: 'Clear', 
                                closure: this.&clearSpec, 
                                mnemonic: 'C', 
                                accelerator: shortcut('ctrl W'))
                        }
                    }
                }
                splitPane(
                    orientation: JSplitPane.VERTICAL_SPLIT,
                    topComponent: scrollPane {
                        widget(
                            scriptEditor = new ConsoleTextEditor(),
                            preferredSize: [600, 300])
                    },
                    bottomComponent: scrollPane {
                        widget(
                            specView = new ConsoleTextEditor(editable: false),
                            preferredSize: [600, 300]    
                        )
                    }
                )    
            }
            mainFrame.pack()
            mainFrame.locationRelativeTo = null
            mainFrame.visible = true
        }
    }
    
    void buildSpec(ActionEvent ae) {
        Thread.start {
            def spec = new AstSpecBuilder().build(scriptEditor.textEditor.text)
            onEdt {
                specView.textEditor.text = spec
            }
        }
    }
    
    void clearSpec(ActionEvent ae) {
        onEdt { specView.textEditor.text = null }
    }
    
    void clearScript(ActionEvent ae) {
        onEdt { scriptEditor.textEditor.text = null }
    }
    
    void onEdt(Closure c) {
        SwingUtilities.isEventDispatchThread() ? 
            c() : SwingUtilities.invokeLater(c)
    }
    
    void exit(ActionEvent ae) {
        mainFrame.dispose()
    }
    
    static void main(args) {
        new AstSpecBrowser().run()    
    }

}
