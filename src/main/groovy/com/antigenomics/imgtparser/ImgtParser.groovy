/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.antigenomics.imgtparser

def cli = new CliBuilder(usage: 'ImgtParser [options] input_imgt_genedb output_file_prefix')
cli.b(longOpt: 'report-bad', 'reports \"bad\" IMGT records, i.e. those that are not V/D/J segment or ' +
        'V and J segments that do not have a reference point (conserved Cys or Phy/Trp)')
cli.n(longOpt: 'non-func', 'include non-functional alleles (ORF and Pseudogene) in output')
cli.m(longOpt: 'minor-allele', 'output minor alleles (*02, *03 and so on)')
cli.h('display help message')

def opt = cli.parse(args)

if (opt == null || opt.arguments().size() < 2) {
    println "[ERROR] Too few arguments provided"
    cli.usage()
    System.exit(-1)
}

if (opt.h) {
    cli.usage()
    System.exit(0)
}


def imgtInputFileName = opt.arguments()[0], outputFilePrefix = opt.arguments()[1]

if (!new File(imgtInputFileName).exists()) {
    println "[ERROR] Input file not exists"
    System.exit(-1)
}

def reader = new FastaReader(imgtInputFileName)
def imgtParser = new ImgtToMigecParser(!opt.n, !opt.m)

def outputFile = new File(outputFilePrefix + ".txt")
if (outputFile.absoluteFile.parentFile)
    outputFile.absoluteFile.parentFile.mkdirs()

outputFile.withPrintWriter { pw ->
    pw.println(MigecSegmentRecord.HEADER)
    reader.each { fastaRecord ->
        def imgtRecord = new ImgtRecord(fastaRecord)
        def migecRecord = imgtParser.parseRecord(imgtRecord)
        if (migecRecord)
            pw.println(migecRecord)
    }
}

if (opt.b) {
    new File(outputFilePrefix + ".novrefpoint").withPrintWriter { pw ->
        imgtParser.failedVReferencePoint.each {
            pw.println(it)
        }
    }
    new File(outputFilePrefix + ".nojrefpoint").withPrintWriter { pw ->
        imgtParser.failedJReferencePoint.each {
            pw.println(it)
        }
    }
    new File(outputFilePrefix + ".othersegm").withPrintWriter { pw ->
        imgtParser.otherSegment.each {
            pw.println(it)
        }
    }
}
new File(outputFilePrefix + ".metadata").withPrintWriter { pw ->
    pw.println(ImgtToMigecParser.HEADER)
    pw.println(imgtParser.toString())
}