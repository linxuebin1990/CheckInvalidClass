package com.res.check

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariantOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import proguard.obfuscate.MappingProcessor
import proguard.obfuscate.MappingReader

import java.nio.charset.StandardCharsets

class CheckInvalidClassComponentTask extends DefaultTask {

    static final String CHARSET = StandardCharsets.UTF_8.name()
    static final String TAG = "CheckInvalidClassComponentTask"

    @Input
    ApplicationVariant applicationVariant

    @Input
    Iterable<String> whiteList

    @Input
    BaseVariantOutput variantOutput

    @TaskAction
    void checkRes() {
        Utils.log TAG, "start check task"

        Map<String, String> map = new LinkedHashMap<>();
        MappingReader reader = new MappingReader(applicationVariant.mappingFile)
        reader.pump(new MappingProcessor() {
            @Override
            boolean processClassMapping(String className, String newClassName) {
                map.put(className, newClassName)
                return false
            }

            @Override
            void processFieldMapping(String className, String fieldType, String fieldName, String newClassName, String newFieldName) {

            }

            @Override
            void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newClassName, int newFirstLineNumber, int newLastLineNumber, String newMethodName) {

            }
        })

        // sort by key length in case of following scenario:
        // key1: me.ele.foo -> me.ele.a
        // key2: me.ele.fooNew -> me.ele.b
        // if we do not sort by length from long to short,
        // the key2 will be mapped to, me.ele.aNew
        map = Utils.sortMapping(map)

        // parse aapt_rules
        def rulesPathCopy = "${project.buildDir.absolutePath}/intermediates/proguard-rules/${applicationVariant.dirName}/aapt_rules_copy_for_check.txt"
        Map<String, Set<String>> replaceMap = Utils.parseAaptRulesFindInvalidClass(rulesPathCopy, map, whiteList)

        boolean hasInvalidClass = false
        replaceMap.each {resMap ->
            if (resMap.value.size() > 0) {
                Utils.log TAG, "Error: ${resMap.key} has invalid class"
                hasInvalidClass = true
                resMap.value.each { invalidClass ->
                    Utils.log TAG, "Error: invalid class : ${invalidClass}"
                }
            }
        }
        Utils.log TAG, "end check task"

        if (hasInvalidClass) {
            throw new RuntimeException("has invalid class show as before.")
        }
    }

}
