package dev.murek.elixirij.ide

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExBackspaceHandlerDelegateTest : BasePlatformTestCase() {

    fun `test backspace removes interpolation closing brace`() {
        myFixture.configureByText("test.ex", "\"#{<caret>}\"")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
        myFixture.checkResult("\"#<caret>\"")
    }
}
