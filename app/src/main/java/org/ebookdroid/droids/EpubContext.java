package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Map;

public class
EpubContext extends PdfContext {

    private static final String TAG = "EpubContext";
    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        LOG.d(TAG, "getCacheFileName", fileNameOriginal, AppSP.get().hypenLang);
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, (fileNameOriginal +
                AppState.get().isReferenceMode +
                AppState.get().isShowPageNumbers +
                AppState.get().isShowFooterNotesInText +
                AppState.get().fullScreenMode +
                //AppState.get().isAccurateFontSize +
                BookCSS.get().documentStyle +
                BookCSS.get().isAutoHypens +
                AppState.get().isBionicMode +
                AppSP.get().hypenLang +
                AppState.get().isExperimental)
                .hashCode() + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        LOG.d(TAG, fileName);

        Map<String, String> notes = null;
        if (AppState.get().isShowFooterNotesInText) {
            notes = getNotes(fileName);
            LOG.d("footer-notes-extracted");
        }

        if ( /** LibreraBuildConfig.DEBUG || **/(BookCSS.get().isAutoHypens || AppState.get().isReferenceMode || AppState.get().isShowFooterNotesInText) && !cacheFile.isFile()) {
            EpubExtractor.proccessHypens(fileName, cacheFile.getPath(), notes);
        }
        if (TempHolder.get().loadingCancelled) {
            removeTempFiles();
            return null;
        }

        String bookPath = (BookCSS.get().isAutoHypens || AppState.get().isReferenceMode || AppState.get().isShowFooterNotesInText) ? cacheFile.getPath() : fileName;

        if (false) {//accelerate open books
            File out = new File(cacheFile.getPath() + "-source");
            if (!AppsConfig.MUPDF_FZ_VERSION.equals(AppsConfig.MUPDF_1_11)) {
                try {
                    if (!out.isDirectory()) {
                        out.mkdirs();
                        new ZipFile(bookPath).extractAll(out.getPath());
                        LOG.d("EpubContext unzip all");

                    }
                    bookPath = out.getPath() + "/META-INF/container.xml";
                    LOG.d("EpubContext open", bookPath);
                } catch (ZipException e) {
                    LOG.e(e);
                }

            }
        }

        final MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, bookPath, password);
        muPdfDocument.cacheFilename = bookPath;

        if (notes != null) {
            muPdfDocument.setFootNotes(notes);
        }

        Thread t = new Thread("@T openDocument") {
            @Override
            public void run() {
                try {

                    if (muPdfDocument.getFootNotes() == null) {
                        muPdfDocument.setFootNotes(getNotes(fileName));
                    }
                    muPdfDocument.setMediaAttachment(EpubExtractor.getAttachments(fileName));

                    //removeTempFiles();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        return muPdfDocument;
    }

    public Map<String, String> getNotes(String fileName) {
        Map<String, String> notes = null;
        final File jsonFile = new File(cacheFile + ".json");
        if (/** !LibreraBuildConfig.DEBUG && **/jsonFile.isFile()) {
            LOG.d("getNotes cache", fileName);
            notes = JsonHelper.fileToMap(jsonFile);
        } else {
            LOG.d("getNotes extract", fileName);
            notes = EpubExtractor.get().getFooterNotes(fileName);
            JsonHelper.mapToFile(jsonFile, notes);
            LOG.d("save notes to file", jsonFile);
        }
        return notes;
    }

}
