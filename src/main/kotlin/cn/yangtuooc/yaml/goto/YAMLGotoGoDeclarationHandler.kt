package cn.yangtuooc.yaml.goto

import com.goide.GoFileType
import com.goide.psi.GoRecursiveVisitor
import com.goide.psi.GoTag
import com.intellij.navigation.DirectNavigationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.impl.YAMLPsiElementImpl

/**
 * @author yangtuo
 */
class YAMLGotoGoDeclarationHandler : DirectNavigationProvider {

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        val fullName = YAMLUtil.getConfigFullName(YAMLPsiElementImpl(element.node))
        return findAllNavigationPairs(element.project)[fullName]
    }

    private fun findAllNavigationPairs(project: Project): MutableMap<String, PsiElement> {
        return CachedValuesManager.getManager(project)
            .getCachedValue(project) {
                val map = mutableMapOf<String, PsiElement>()
                val goFiles = FileTypeIndex.getFiles(GoFileType.INSTANCE, GlobalSearchScope.projectScope(project))
                goFiles.forEach { virtualFile ->
                    val goFile = PsiManager.getInstance(project).findFile(virtualFile)
                    goFile?.accept(object : GoRecursiveVisitor() {
                        override fun visitTag(o: GoTag) {
                            super.visitTag(o)
                            if (o.reference != null) {
                                visitTag(o)
                            }
                            //fixme: 调整匹配策略
                            val tag = o.text
                            if (tag.contains("yaml")) {
                                val key = tag.substring(tag.indexOf("yaml:\"") + 6, tag.lastIndexOf("\""))
                                map[key] = o
                            }
                        }
                    })
                }

                CachedValueProvider.Result.create(map, listOf(project))
            }
    }

}