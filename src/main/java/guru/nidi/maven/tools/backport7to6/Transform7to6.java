/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.maven.tools.backport7to6;

import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 */
class Transform7to6 extends ClassVisitor implements Opcodes {
    private final String filename;

    private Transform7to6(String filename, ClassVisitor cv) {
        super(ASM5, cv);
        this.filename = filename;
    }

    public static boolean transform(File file, String filename) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        final ClassReader in = new ClassReader(fis);
        final ClassWriter out = new ContextClassloaderClassWriter(in, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        final Transform7to6 transform7to6 = new Transform7to6(filename, out);
        try {
            in.accept(transform7to6, 0);
            final FileOutputStream fos = new FileOutputStream(file);
            fos.write(out.toByteArray());
            fos.close();
            return true;
        } catch (NoConversionNeededException e) {
            return false;
        } finally {
            fis.close();
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (version > 0x33) {
            throw new IllegalStateException(filename + " has a version > 7. Cannot be converted.");
        }
        if (version < 0x33) {
            throw new NoConversionNeededException();
        }
        super.visit(0x32, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new Jdk7RemovingVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    private static class NoConversionNeededException extends RuntimeException {
    }

    private static class Jdk7RemovingVisitor extends MethodVisitor implements Opcodes {
        public Jdk7RemovingVisitor(MethodVisitor mv) {
            super(ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (owner.equals("java/lang/Throwable") && name.equals("addSuppressed")) {
                super.visitInsn(POP2);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

    private static class ContextClassloaderClassWriter extends ClassWriter {
        public ContextClassloaderClassWriter(ClassReader classReader, int flags) {
            super(classReader, flags);
        }

        protected String getCommonSuperClass(final String type1, final String type2) {
            Class<?> c, d;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                c = Class.forName(type1.replace('/', '.'), false, classLoader);
                d = Class.forName(type2.replace('/', '.'), false, classLoader);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
            if (c.isAssignableFrom(d)) {
                return type1;
            }
            if (d.isAssignableFrom(c)) {
                return type2;
            }
            if (c.isInterface() || d.isInterface()) {
                return "java/lang/Object";
            }
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
}

