package org.codehaus.plexus.archiver.tar;

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.File;

/**
 * list files in tar with bzip2 compression
 * @author Franck Bonin
 * @version $Revision$ $Date$
 */
public class TarBZip2ArchiveContentLister
    extends TarArchiveContentLister
{
    public TarBZip2ArchiveContentLister()
    {
        this.setupCompressionMethod();
    }

    public TarBZip2ArchiveContentLister( File sourceFile )
    {
        super( sourceFile );
        this.setupCompressionMethod();
    }

    private void setupCompressionMethod()
    {
		    this.setCompression(UntarCompressionMethod.BZIP2);
    }
    
}
