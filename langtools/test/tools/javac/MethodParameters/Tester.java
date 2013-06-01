/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Test driver for MethodParameters testing.
 * <p>
 * The intended use of this driver is to run it, giving the name of
 * a single class compiled with -parameters as argument. The driver
 * will test the specified class, and any nested classes it finds.
 * <p>
 * Each class is tested in two way. By refelction, and by directly
 * checking MethodParameters attributes in the classfile. The checking
 * is done using two visitor classes {@link ClassFileVisitor} and
 * {@link ReflectionVisitor}.
 * <p>
 * The {@code ReflectionVisitor} test logically belongs with library tests.
 * we wish to reuse the same test-cases, so both test are committed together,
 * under langtools. The tests, may be duplicated in the jdk repository.
 */
public class Tester {

    final static File classesdir = new File(System.getProperty("test.classes", "."));

    /**
     * The visitor classes that does the actual checking are referenced
     * statically, to force compilations, without having to reference
     * them in individual test cases.
     * <p>
     * This makes it easy to change the set of visitors, without
     * complicating the design with dynamic discovery and compilation
     * of visitor classes.
     */
    static final Class visitors[] = {
        ClassFileVisitor.class,
        ReflectionVisitor.class
    };

    /**
     * Test-driver expect a single classname as argument.
     */
    public static void main(String... args) throws Exception {
        if (args.length != 1) {
            throw new Error("A single class name is expected as argument");
        }
        final String pattern = args[0] + ".*\\.class";
        File files[] = classesdir.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().matches(pattern);
                }
            });
        if (files.length == 0) {
            File file = new File(classesdir, args[0] + ".class");
            throw new Error(file.getPath() + " not found");
        }

        new Tester(args[0], files).run();
    }

    public Tester(String name, File files[]) {
        this.classname = name;
        this.files = files;
    }

    void run() throws Exception {

        // Test with each visitor
        for (Class<Visitor> vclass : visitors) {
            try {
                String vname = vclass.getName();
                Constructor c = vclass.getConstructor(Tester.class);

                info("\nRun " + vname + " for " + classname + "\n");
                StringBuilder sb = new StringBuilder();
                for (File f : files) {
                    String fname = f.getName();
                    fname = fname.substring(0, fname.length() - 6);
                    Visitor v = (Visitor) c.newInstance(this);
                    try {
                        v.visitClass(fname, f,  sb);
                    } catch(Exception e) {
                        error("Uncaught exception in visitClass()");
                        e.printStackTrace();
                    }
                }
                info(sb.toString());
            } catch(ReflectiveOperationException e) {
                warn("Class " + vclass.getName() + " ignored, not a Visitor");
                continue;
            }
        }

        if(0 != warnings)
                System.err.println("Test generated " + warnings + " warnings");

        if(0 != errors)
            throw new Exception("Tester test failed with " +
                                errors + " errors");
    }

    abstract static  class Visitor {
        Tester tester;
        File classesdir;

        public Visitor(Tester tester) {
            this.tester = tester;
        }

        abstract void visitClass(final String classname, final File  cfile,
                final StringBuilder sb) throws Exception;

        public void error(String msg) {
            tester.error(msg);
        }

        public void warn(String msg) {
            tester.warn(msg);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    void warn(String msg) {
        System.err.println("Warning: " + msg);
        warnings++;
    }

    void info(String msg) {
        System.out.println(msg);
    }

    int errors;
    int warnings;
    String classname;
    File files[];
}
