// Copyright (c) 2016 Uber
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.uber.jenkins.phabricator.coverage;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import hudson.FilePath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class PathResolverTest {
    public List<String> candidates;
    public List<String> filenames;
    public Map<String, String> expectedResult;

    private Stack<File> cleanupPaths;

    public PathResolverTest(List<String> candidates, List<String> filenames, Map<String, String> expectedResult) {
        this.candidates = candidates;
        this.filenames = filenames;
        this.expectedResult = expectedResult;
    }

    @Test
    public void testChoose() throws Exception {
        File tmpDir = Files.createTempDir();
        cleanupPaths.push(tmpDir);
        List<String> dirs = new ArrayList<String>();
        for (String path : candidates) {
            if (path.isEmpty()) {
                continue;
            }
            File file = new File(tmpDir, path);
            if (path.endsWith("/")) {
                file.mkdirs();
                dirs.add(path);
            } else {
                file.createNewFile();
            }
            cleanupPaths.push(file);
        }

        Map<String, String> chosen = new PathResolver(new FilePath(tmpDir), dirs).choose(filenames);

        for (String filename  : filenames) {
            assertEquals(expectedResult.get(filename), chosen.get(filename));
        }
    }

    @Before
    public void setUp() {
        cleanupPaths = new Stack<File>();
    }

    @After
    public void tearDown() {
        for (File path : cleanupPaths) {
            path.delete();
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> result = new ArrayList();
        result.add(new Object[] { Collections.emptyList(), Arrays.asList("dir/file"), ImmutableMap.<String, String>of()});
        result.add(new Object[] { Arrays.asList("workspace/", "workspace/dir/", "workspace/dir/file"), Arrays.asList("dir/file"), ImmutableMap.<String, String>of("dir/file", "workspace/") });
        result.add(new Object[] { Arrays.asList("workspace/", "workspace/dir/", "workspace/dir/dir/", "workspace/dir/file"), Arrays.asList("dir/file"), ImmutableMap.<String, String>of("dir/file", "workspace/")});
        result.add(new Object[] { Arrays.asList("workspace/", "workspace/dir/", "workspace/dir/dir/", "workspace/dir/file", "workspace2/dir/", "workspace2/dir/file2"), Arrays.asList("dir/file", "file2"), ImmutableMap.<String, String>of("dir/file", "workspace/", "file2", "workspace2/dir/") });
        return result;
    }
}
