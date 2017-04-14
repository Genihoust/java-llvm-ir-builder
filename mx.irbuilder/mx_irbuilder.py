import os

import mx
import mx_sulong
import mx_testsuites
import argparse
import sys

_suite = mx.suite('irbuilder')

class Tool(object):
    def supports(self, language):
        return language in self.supportedLanguages

    def runTool(self, args, errorMsg=None, verbose=None, **kwargs):
        try:
            if not mx.get_opts().verbose and not verbose:
                f = open(os.devnull, 'w')
                ret = mx.run(args, out=f, err=f, **kwargs)
            else:
                f = None
                ret = mx.run(args, **kwargs)
        except SystemExit:
            ret = -1
            if errorMsg is None:
                mx.log_error()
                mx.log_error('Error: Cannot run {}'.format(args))
            else:
                mx.log_error()
                mx.log_error('Error: {}'.format(errorMsg))
                mx.log_error(' '.join(args))
        if f is not None:
            f.close()
        return ret

class LlvmAS(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = mx_sulong.findLLVMProgram('llvm-as', self.supportedVersions)
        return self.runTool([tool] + flags + [inputFile], errorMsg='Cannot assemble %s with %s' % (inputFile, tool), verbose=True)

class LlvmLLI(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = mx_sulong.findLLVMProgram('lli', self.supportedVersions)
        return self.runTool([tool] + flags + [inputFile], nonZeroIsFatal=False, errorMsg='Cannot run %s with %s' % (inputFile, tool))

def getIRWriterClasspathOptions():
    """gets the classpath of the IRWRITER distributions"""
    return mx.get_runtime_jvm_args('IRWRITER')

def runIRBuilderOut(args=None, out=None):
    return mx.run_java(getIRWriterClasspathOptions() + ["at.pointhi.irbuilder.irwriter.SourceParser"] + args)

irBuilderTests32 = {
    'sulong' : ['sulong', "at.pointhi.irbuilder.test.SulongGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulong')],
    'llvm' : ['llvm', "at.pointhi.irbuilder.test.LLVMGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'llvm')],
    'gcc' : ['gcc', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc')],
    'nwcc' : ['nwcc', "at.pointhi.irbuilder.test.NWCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'nwcc')],
    'assembly' : ['assembly', "at.pointhi.irbuilder.test.InlineAssemblyGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'inlineassemblytests')],
}

irBuilderTests38 = {
    'sulong' : ['sulong38', "at.pointhi.irbuilder.test.SulongGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulong')],
    'sulongcpp' : ['sulongcpp38', "at.pointhi.irbuilder.test.SulongCPPGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulongcpp')],
    'llvm' : ['llvm38', "at.pointhi.irbuilder.test.LLVMGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'llvm')],
    'gcc' : ['gcc38', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc')],
    'nwcc' : ['nwcc38', "at.pointhi.irbuilder.test.NWCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'nwcc')],
}

def runIRBuilderTest32(vmArgs):
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests32.keys()), default=irBuilderTests32.keys())
    parsedArgs = parser.parse_args(otherArgs)

    returnCode = 0
    for testSuiteName in parsedArgs.suite:
        suite = irBuilderTests32[testSuiteName]
        """runs the test suite"""
        mx_sulong.ensureDragonEggExists()
        mx_sulong.mx_testsuites.compileSuite([suite[0]])
        try:
            mx_sulong.mx_testsuites.run32(vmArgs, suite[1], [])
        except:
            pass
        if _runIRGeneratorSuite(LlvmAS(['3.2', '3.3']), LlvmLLI(['3.2', '3.3']), suite[2]) != 0:
            returnCode = 1

    return returnCode

def runIRBuilderTest38(vmArgs):
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests38.keys()), default=irBuilderTests38.keys())
    parsedArgs = parser.parse_args(otherArgs)

    returnCode = 0
    for testSuiteName in parsedArgs.suite:
        suite = irBuilderTests38[testSuiteName]
        """runs the test suite"""
        mx_sulong.ensureDragonEggExists()
        mx_sulong.mx_testsuites.compileSuite([suite[0]])
        try:
            mx_sulong.mx_testsuites.run38(vmArgs, suite[1], [])
        except:
            pass
        if _runIRGeneratorSuite(LlvmAS(['3.8', '3.9']), LlvmLLI(['3.8', '3.9']), suite[2]) != 0:
            returnCode = 1

    return returnCode

def _runIRGeneratorSuite(assembler, lli, sulongSuiteCacheDir):
    mx.log('Testing Reassembly')
    mx.log(sulongSuiteCacheDir)
    failed = []
    passed = []
    for root, _, files in os.walk(sulongSuiteCacheDir):
        for fileName in files:
            inputFile = os.path.join(sulongSuiteCacheDir, root, fileName)
            if inputFile.endswith('.out.ll'):
                if assembler.run(inputFile) == 0:
                    exit_code_ref = lli.run(inputFile[:-7] + ".bc")
                    exit_code_out = lli.run(inputFile[:-7] + ".out.bc")
                    if exit_code_ref == exit_code_out:
                        sys.stdout.write('.')
                        passed.append(inputFile)
                        sys.stdout.flush()
                    else:
                        sys.stdout.write('E')
                        failed.append(inputFile)
                        sys.stdout.flush()
                else:
                    sys.stdout.write('E')
                    failed.append(inputFile)
                    sys.stdout.flush()
    total = len(failed) + len(passed)
    mx.log()
    if len(failed) != 0:
        mx.log_error('Failed ' + str(len(failed)) + ' of ' + str(total) + ' Tests!')
        for x in range(0, len(failed)):
            mx.log_error(str(x) + ') ' + failed[x])
        return 1
    elif total == 0:
        mx.log_error('There is something odd with the testsuite, ' + str(total) + ' Tests executed!')
        return 1
    else:
        mx.log('Passed all ' + str(total) + ' Tests!')
        return 0

mx.update_commands(_suite, {
    'irbuilder-out' : [runIRBuilderOut, ''],
    'irbuilder-test32' : [runIRBuilderTest32, ''],
    'irbuilder-test38' : [runIRBuilderTest38, ''],
})

