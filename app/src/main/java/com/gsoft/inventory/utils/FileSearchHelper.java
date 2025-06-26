package com.gsoft.inventory.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.gsoft.inventory.InventoryApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearchHelper {
    /**
     * -- MIME 列表 --
     */
    private static final String[][] MIMETable =
            {
                    // --{后缀名， MIME类型}   --
                    {"3gp", "video/3gpp"},
                    {"3gpp", "video/3gpp"},
                    {"aac", "audio/x-mpeg"},
                    {"amr", "audio/x-mpeg"},
                    {"apk", "application/vndandroidpackage-archive"},
                    {"avi", "video/x-msvideo"},
                    {"aab", "application/x-authoware-bin"},
                    {"aam", "application/x-authoware-map"},
                    {"aas", "application/x-authoware-seg"},
                    {"ai", "application/postscript"},
                    {"aif", "audio/x-aiff"},
                    {"aifc", "audio/x-aiff"},
                    {"aiff", "audio/x-aiff"},
                    {"als", "audio/x-alpha5"},
                    {"amc", "application/x-mpeg"},
                    {"ani", "application/octet-stream"},
                    {"asc", "text/plain"},
                    {"asd", "application/astound"},
                    {"asf", "video/x-ms-asf"},
                    {"asn", "application/astound"},
                    {"asp", "application/x-asap"},
                    {"asx", " video/x-ms-asf"},
                    {"au", "audio/basic"},
                    {"avb", "application/octet-stream"},
                    {"awb", "audio/amr-wb"},
                    {"bcpio", "application/x-bcpio"},
                    {"bld", "application/bld"},
                    {"bld2", "application/bld2"},
                    {"bpk", "application/octet-stream"},
                    {"bz2", "application/x-bzip2"},
                    {"bin", "application/octet-stream"},
                    {"bmp", "image/bmp"},
                    {"c", "text/plain"},
                    {"class", "application/octet-stream"},
                    {"conf", "text/plain"},
                    {"cpp", "text/plain"},
                    {"cal", "image/x-cals"},
                    {"ccn", "application/x-cnc"},
                    {"cco", "application/x-cocoa"},
                    {"cdf", "application/x-netcdf"},
                    {"cgi", "magnus-internal/cgi"},
                    {"chat", "application/x-chat"},
                    {"clp", "application/x-msclip"},
                    {"cmx", "application/x-cmx"},
                    {"co", "application/x-cult3d-object"},
                    {"cod", "image/cis-cod"},
                    {"cpio", "application/x-cpio"},
                    {"cpt", "application/mac-compactpro"},
                    {"crd", "application/x-mscardfile"},
                    {"csh", "application/x-csh"},
                    {"csm", "chemical/x-csml"},
                    {"csml", "chemical/x-csml"},
                    {"css", "text/css"},
                    {"cur", "application/octet-stream"},
                    {"doc", "application/msword"},
                    {"dcm", "x-lml/x-evm"},
                    {"dcr", "application/x-director"},
                    {"dcx", "image/x-dcx"},
                    {"dhtml", "text/html"},
                    {"dir", "application/x-director"},
                    {"dll", "application/octet-stream"},
                    {"dmg", "application/octet-stream"},
                    {"dms", "application/octet-stream"},
                    {"dot", "application/x-dot"},
                    {"dvi", "application/x-dvi"},
                    {"dwf", "drawing/x-dwf"},
                    {"dwg", "application/x-autocad"},
                    {"dxf", "application/x-autocad"},
                    {"dxr", "application/x-director"},
                    {"ebk", "application/x-expandedbook"},
                    {"emb", "chemical/x-embl-dl-nucleotide"},
                    {"embl", "chemical/x-embl-dl-nucleotide"},
                    {"eps", "application/postscript"},
                    {"epub", "application/epub+zip"},
                    {"eri", "image/x-eri"},
                    {"es", "audio/echospeech"},
                    {"esl", "audio/echospeech"},
                    {"etc", "application/x-earthtime"},
                    {"etx", "text/x-setext"},
                    {"evm", "x-lml/x-evm"},
                    {"evy", "application/x-envoy"},
                    {"exe", "application/octet-stream"},
                    {"fh4", "image/x-freehand"},
                    {"fh5", "image/x-freehand"},
                    {"fhc", "image/x-freehand"},
                    {"fif", "image/fif"},
                    {"fm", "application/x-maker"},
                    {"fpx", "image/x-fpx"},
                    {"fvi", "video/isivideo"},
                    {"flv", "video/x-msvideo"},
                    {"gau", "chemical/x-gaussian-input"},
                    {"gca", "application/x-gca-compressed"},
                    {"gdb", "x-lml/x-gdb"},
                    {"gif", "image/gif"},
                    {"gps", "application/x-gps"},
                    {"gtar", "application/x-gtar"},
                    {"gz", "application/x-gzip"},
                    {"gif", "image/gif"},
                    {"gtar", "application/x-gtar"},
                    {"gz", "application/x-gzip"},
                    {"h", "text/plain"},
                    {"hdf", "application/x-hdf"},
                    {"hdm", "text/x-hdml"},
                    {"hdml", "text/x-hdml"},
                    {"htm", "text/html"},
                    {"html", "text/html"},
                    {"hlp", "application/winhlp"},
                    {"hqx", "application/mac-binhex40"},
                    {"hts", "text/html"},
                    {"ice", "x-conference/x-cooltalk"},
                    {"ico", "application/octet-stream"},
                    {"ief", "image/ief"},
                    {"ifm", "image/gif"},
                    {"ifs", "image/ifs"},
                    {"imy", "audio/melody"},
                    {"ins", "application/x-net-install"},
                    {"ips", "application/x-ipscript"},
                    {"ipx", "application/x-ipix"},
                    {"it", "audio/x-mod"},
                    {"itz", "audio/x-mod"},
                    {"ivr", "i-world/i-vrml"},
                    {"j2k", "image/j2k"},
                    {"jad", "text/vndsunj2meapp-descriptor"},
                    {"jam", "application/x-jam"},
                    {"jnlp", "application/x-java-jnlp-file"},
                    {"jpe", "image/jpeg"},
                    {"jpz", "image/jpeg"},
                    {"jwc", "application/jwc"},
                    {"jar", "application/java-archive"},
                    {"java", "text/plain"},
                    {"jpeg", "image/jpeg"},
                    {"jpg", "image/jpeg"},
                    {"js", "application/x-javascript"},
                    {"kjx", "application/x-kjx"},
                    {"lak", "x-lml/x-lak"},
                    {"latex", "application/x-latex"},
                    {"lcc", "application/fastman"},
                    {"lcl", "application/x-digitalloca"},
                    {"lcr", "application/x-digitalloca"},
                    {"lgh", "application/lgh"},
                    {"lha", "application/octet-stream"},
                    {"lml", "x-lml/x-lml"},
                    {"lmlpack", "x-lml/x-lmlpack"},
                    {"log", "text/plain"},
                    {"lsf", "video/x-ms-asf"},
                    {"lsx", "video/x-ms-asf"},
                    {"lzh", "application/x-lzh "},
                    {"m13", "application/x-msmediaview"},
                    {"m14", "application/x-msmediaview"},
                    {"m15", "audio/x-mod"},
                    {"m3u", "audio/x-mpegurl"},
                    {"m3url", "audio/x-mpegurl"},
                    {"ma1", "audio/ma1"},
                    {"ma2", "audio/ma2"},
                    {"ma3", "audio/ma3"},
                    {"ma5", "audio/ma5"},
                    {"man", "application/x-troff-man"},
                    {"map", "magnus-internal/imagemap"},
                    {"mbd", "application/mbedlet"},
                    {"mct", "application/x-mascot"},
                    {"mdb", "application/x-msaccess"},
                    {"mdz", "audio/x-mod"},
                    {"me", "application/x-troff-me"},
                    {"mel", "text/x-vmel"},
                    {"mi", "application/x-mif"},
                    {"mid", "audio/midi"},
                    {"midi", "audio/midi"},
                    {"m4a", "audio/mp4a-latm"},
                    {"m4b", "audio/mp4a-latm"},
                    {"m4p", "audio/mp4a-latm"},
                    {"m4u", "video/vndmpegurl"},
                    {"m4v", "video/x-m4v"},
                    {"mov", "video/quicktime"},
                    {"mp2", "audio/x-mpeg"},
                    {"mp3", "audio/x-mpeg"},
                    {"mp4", "video/mp4"},
                    {"mpc", "application/vndmpohuncertificate"},
                    {"mpe", "video/mpeg"},
                    {"mpeg", "video/mpeg"},
                    {"mpg", "video/mpeg"},
                    {"mpg4", "video/mp4"},
                    {"mpga", "audio/mpeg"},
                    {"msg", "application/vndms-outlook"},
                    {"mif", "application/x-mif"},
                    {"mil", "image/x-cals"},
                    {"mio", "audio/x-mio"},
                    {"mmf", "application/x-skt-lbs"},
                    {"mng", "video/x-mng"},
                    {"mny", "application/x-msmoney"},
                    {"moc", "application/x-mocha"},
                    {"mocha", "application/x-mocha"},
                    {"mod", "audio/x-mod"},
                    {"mof", "application/x-yumekara"},
                    {"mol", "chemical/x-mdl-molfile"},
                    {"mop", "chemical/x-mopac-input"},
                    {"movie", "video/x-sgi-movie"},
                    {"mpn", "application/vndmophunapplication"},
                    {"mpp", "application/vndms-project"},
                    {"mps", "application/x-mapserver"},
                    {"mrl", "text/x-mrml"},
                    {"mrm", "application/x-mrm"},
                    {"ms", "application/x-troff-ms"},
                    {"mts", "application/metastream"},
                    {"mtx", "application/metastream"},
                    {"mtz", "application/metastream"},
                    {"mzv", "application/metastream"},
                    {"nar", "application/zip"},
                    {"nbmp", "image/nbmp"},
                    {"nc", "application/x-netcdf"},
                    {"ndb", "x-lml/x-ndb"},
                    {"ndwn", "application/ndwn"},
                    {"nif", "application/x-nif"},
                    {"nmz", "application/x-scream"},
                    {"nokia-op-logo", "image/vndnok-oplogo-color"},
                    {"npx", "application/x-netfpx"},
                    {"nsnd", "audio/nsnd"},
                    {"nva", "application/x-neva1"},
                    {"oda", "application/oda"},
                    {"oom", "application/x-atlasMate-plugin"},
                    {"ogg", "audio/ogg"},
                    {"pac", "audio/x-pac"},
                    {"pae", "audio/x-epac"},
                    {"pan", "application/x-pan"},
                    {"pbm", "image/x-portable-bitmap"},
                    {"pcx", "image/x-pcx"},
                    {"pda", "image/x-pda"},
                    {"pdb", "chemical/x-pdb"},
                    {"pdf", "application/pdf"},
                    {"pfr", "application/font-tdpfr"},
                    {"pgm", "image/x-portable-graymap"},
                    {"pict", "image/x-pict"},
                    {"pm", "application/x-perl"},
                    {"pmd", "application/x-pmd"},
                    {"png", "image/png"},
                    {"pnm", "image/x-portable-anymap"},
                    {"pnz", "image/png"},
                    {"pot", "application/vndms-powerpoint"},
                    {"ppm", "image/x-portable-pixmap"},
                    {"pps", "application/vndms-powerpoint"},
                    {"ppt", "application/vndms-powerpoint"},
                    {"pqf", "application/x-cprplayer"},
                    {"pqi", "application/cprplayer"},
                    {"prc", "application/x-prc"},
                    {"proxy", "application/x-ns-proxy-autoconfig"},
                    {"prop", "text/plain"},
                    {"ps", "application/postscript"},
                    {"ptlk", "application/listenup"},
                    {"pub", "application/x-mspublisher"},
                    {"pvx", "video/x-pv-pvx"},
                    {"qcp", "audio/vndqcelp"},
                    {"qt", "video/quicktime"},
                    {"qti", "image/x-quicktime"},
                    {"qtif", "image/x-quicktime"},
                    {"r3t", "text/vndrn-realtext3d"},
                    {"ra", "audio/x-pn-realaudio"},
                    {"ram", "audio/x-pn-realaudio"},
                    {"ras", "image/x-cmu-raster"},
                    {"rdf", "application/rdf+xml"},
                    {"rf", "image/vndrn-realflash"},
                    {"rgb", "image/x-rgb"},
                    {"rlf", "application/x-richlink"},
                    {"rm", "audio/x-pn-realaudio"},
                    {"rmf", "audio/x-rmf"},
                    {"rmm", "audio/x-pn-realaudio"},
                    {"rnx", "application/vndrn-realplayer"},
                    {"roff", "application/x-troff"},
                    {"rp", "image/vndrn-realpix"},
                    {"rpm", "audio/x-pn-realaudio-plugin"},
                    {"rt", "text/vndrn-realtext"},
                    {"rte", "x-lml/x-gps"},
                    {"rtf", "application/rtf"},
                    {"rtg", "application/metastream"},
                    {"rtx", "text/richtext"},
                    {"rv", "video/vndrn-realvideo"},
                    {"rwc", "application/x-rogerwilco"},
                    {"rar", "application/rar"},
                    {"rc", "text/plain"},
                    {"rmvb", "audio/x-pn-realaudio"},
                    {"s3m", "audio/x-mod"},
                    {"s3z", "audio/x-mod"},
                    {"sca", "application/x-supercard"},
                    {"scd", "application/x-msschedule"},
                    {"sdf", "application/e-score"},
                    {"sea", "application/x-stuffit"},
                    {"sgm", "text/x-sgml"},
                    {"sgml", "text/x-sgml"},
                    {"shar", "application/x-shar"},
                    {"shtml", "magnus-internal/parsed-html"},
                    {"shw", "application/presentations"},
                    {"si6", "image/si6"},
                    {"si7", "image/vndstiwapsis"},
                    {"si9", "image/vndlgtwapsis"},
                    {"sis", "application/vndsymbianinstall"},
                    {"sit", "application/x-stuffit"},
                    {"skd", "application/x-koan"},
                    {"skm", "application/x-koan"},
                    {"skp", "application/x-koan"},
                    {"skt", "application/x-koan"},
                    {"slc", "application/x-salsa"},
                    {"smd", "audio/x-smd"},
                    {"smi", "application/smil"},
                    {"smil", "application/smil"},
                    {"smp", "application/studiom"},
                    {"smz", "audio/x-smd"},
                    {"sh", "application/x-sh"},
                    {"snd", "audio/basic"},
                    {"spc", "text/x-speech"},
                    {"spl", "application/futuresplash"},
                    {"spr", "application/x-sprite"},
                    {"sprite", "application/x-sprite"},
                    {"sdp", "application/sdp"},
                    {"spt", "application/x-spt"},
                    {"src", "application/x-wais-source"},
                    {"stk", "application/hyperstudio"},
                    {"stm", "audio/x-mod"},
                    {"sv4cpio", "application/x-sv4cpio"},
                    {"sv4crc", "application/x-sv4crc"},
                    {"svf", "image/vnd"},
                    {"svg", "image/svg-xml"},
                    {"svh", "image/svh"},
                    {"svr", "x-world/x-svr"},
                    {"swf", "application/x-shockwave-flash"},
                    {"swfl", "application/x-shockwave-flash"},
                    {"t", "application/x-troff"},
                    {"tad", "application/octet-stream"},
                    {"talk", "text/x-speech"},
                    {"tar", "application/x-tar"},
                    {"taz", "application/x-tar"},
                    {"tbp", "application/x-timbuktu"},
                    {"tbt", "application/x-timbuktu"},
                    {"tcl", "application/x-tcl"},
                    {"tex", "application/x-tex"},
                    {"texi", "application/x-texinfo"},
                    {"texinfo", "application/x-texinfo"},
                    {"tgz", "application/x-tar"},
                    {"thm", "application/vnderithm"},
                    {"tif", "image/tiff"},
                    {"tiff", "image/tiff"},
                    {"tki", "application/x-tkined"},
                    {"tkined", "application/x-tkined"},
                    {"toc", "application/toc"},
                    {"toy", "image/toy"},
                    {"tr", "application/x-troff"},
                    {"trk", "x-lml/x-gps"},
                    {"trm", "application/x-msterminal"},
                    {"tsi", "audio/tsplayer"},
                    {"tsp", "application/dsptype"},
                    {"tsv", "text/tab-separated-values"},
                    {"ttf", "application/octet-stream"},
                    {"ttz", "application/t-time"},
                    {"txt", "text/plain"},
                    {"ult", "audio/x-mod"},
                    {"ustar", "application/x-ustar"},
                    {"uu", "application/x-uuencode"},
                    {"uue", "application/x-uuencode"},
                    {"vcd", "application/x-cdlink"},
                    {"vcf", "text/x-vcard"},
                    {"vdo", "video/vdo"},
                    {"vib", "audio/vib"},
                    {"viv", "video/vivo"},
                    {"vivo", "video/vivo"},
                    {"vmd", "application/vocaltec-media-desc"},
                    {"vmf", "application/vocaltec-media-file"},
                    {"vmi", "application/x-dreamcast-vms-info"},
                    {"vms", "application/x-dreamcast-vms"},
                    {"vox", "audio/voxware"},
                    {"vqe", "audio/x-twinvq-plugin"},
                    {"vqf", "audio/x-twinvq"},
                    {"vql", "audio/x-twinvq"},
                    {"vre", "x-world/x-vream"},
                    {"vrml", "x-world/x-vrml"},
                    {"vrt", "x-world/x-vrt"},
                    {"vrw", "x-world/x-vream"},
                    {"vts", "workbook/formulaone"},
                    {"wax", "audio/x-ms-wax"},
                    {"wbmp", "image/vndwapwbmp"},
                    {"web", "application/vndxara"},
                    {"wav", "audio/x-wav"},
                    {"wma", "audio/x-ms-wma"},
                    {"wmv", "audio/x-ms-wmv"},
                    {"wi", "image/wavelet"},
                    {"wis", "application/x-InstallShield"},
                    {"wm", "video/x-ms-wm"},
                    {"wmd", "application/x-ms-wmd"},
                    {"wmf", "application/x-msmetafile"},
                    {"wml", "text/vndwapwml"},
                    {"wmlc", "application/vndwapwmlc"},
                    {"wmls", "text/vndwapwmlscript"},
                    {"wmlsc", "application/vndwapwmlscriptc"},
                    {"wmlscript", "text/vndwapwmlscript"},
                    {"wmv", "video/x-ms-wmv"},
                    {"wmx", "video/x-ms-wmx"},
                    {"wmz", "application/x-ms-wmz"},
                    {"wpng", "image/x-up-wpng"},
                    {"wps", "application/vndms-works"},
                    {"wpt", "x-lml/x-gps"},
                    {"wri", "application/x-mswrite"},
                    {"wrl", "x-world/x-vrml"},
                    {"wrz", "x-world/x-vrml"},
                    {"ws", "text/vndwapwmlscript"},
                    {"wsc", "application/vndwapwmlscriptc"},
                    {"wv", "video/wavelet"},
                    {"wvx", "video/x-ms-wvx"},
                    {"wxl", "application/x-wxl"},
                    {"x-gzip", "application/x-gzip"},
                    {"xar", "application/vndxara"},
                    {"xbm", "image/x-xbitmap"},
                    {"xdm", "application/x-xdma"},
                    {"xdma", "application/x-xdma"},
                    {"xdw", "application/vndfujixeroxdocuworks"},
                    {"xht", "application/xhtml+xml"},
                    {"xhtm", "application/xhtml+xml"},
                    {"xhtml", "application/xhtml+xml"},
                    {"xla", "application/vndms-excel"},
                    {"xlc", "application/vndms-excel"},
                    {"xll", "application/x-excel"},
                    {"xlm", "application/vndms-excel"},
                    {"xls", "application/vndms-excel"},
                    {"xlt", "application/vndms-excel"},
                    {"xlw", "application/vndms-excel"},
                    {"xm", "audio/x-mod"},
                    {"xml", "text/xml"},
                    {"xmz", "audio/x-mod"},
                    {"xpi", "application/x-xpinstall"},
                    {"xpm", "image/x-xpixmap"},
                    {"xsit", "text/xml"},
                    {"xsl", "text/xml"},
                    {"xul", "text/xul"},
                    {"xwd", "image/x-xwindowdump"},
                    {"xyz", "chemical/x-pdb"},
                    {"yz1", "application/x-yz1"},
                    {"z", "application/x-compress"},
                    {"zac", "application/x-zaurus-zac"},
                    {"zip", "application/zip"},
                    {"", "*/*"}
            };

    public static List searchType(Context context, String type) {
        List fileList = new ArrayList<>();
        String t = "";
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        for (int i = 0; i < MIMETable.length; i++) {
            if (type.equals(MIMETable[i][0])) {
                t = MIMETable[i][1];
            }
        }
        Cursor cursor = resolver.query(uri,
                new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
                MediaStore.Files.FileColumns.MIME_TYPE + " = '" + t + "'",
                null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                FileBean bean = new FileBean();
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                bean.setName(path.substring(path.lastIndexOf("/") + 1));
                bean.setPath(path);
                bean.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.SIZE)));
                bean.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DATE_MODIFIED)));
                fileList.add(bean);
            }
        }
        cursor.close();
        return fileList;
    }

    public static List<FileBean> searchType(Context context, Uri searchUri, String type) {
        List<FileBean> fileList = new ArrayList<>();
        String t = "";
        ContentResolver resolver = context.getContentResolver();

        for (int i = 0; i < MIMETable.length; i++) {
            if (type.equals(MIMETable[i][0])) {
                t = MIMETable[i][1];
            }
        }
        Cursor cursor = resolver.query(searchUri,
                new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
                MediaStore.Files.FileColumns.MIME_TYPE + " = '" + t + "'",
                null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                FileBean bean = new FileBean();
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                bean.setName(path.substring(path.lastIndexOf("/") + 1));
                bean.setPath(path);
                bean.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.SIZE)));
                bean.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DATE_MODIFIED)));
                fileList.add(bean);
            }

            cursor.close();
        }

        return fileList;
    }

    public static List searchKeyWord(Context context, String keyword) {
        List fileList = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = resolver.query(uri,
                new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
                MediaStore.Files.FileColumns.TITLE + " LIKE '%" + keyword + "%'",
                null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                FileBean bean = new FileBean();
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                bean.setName(path.substring(path.lastIndexOf("/") + 1));
                bean.setPath(path);
                bean.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.SIZE)));
                bean.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DATE_MODIFIED)));
                fileList.add(bean);
            }
        }
        cursor.close();
        return fileList;
    }

    public static List searchKeyWord(Context context, Uri searchUri, String keyword) {
        List fileList = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        Cursor cursor = resolver.query(searchUri,
                new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
                MediaStore.Files.FileColumns.TITLE + " LIKE '%" + keyword + "%'",
                null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                FileBean bean = new FileBean();
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                bean.setName(path.substring(path.lastIndexOf("/") + 1));
                bean.setPath(path);
                bean.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.SIZE)));
                bean.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DATE_MODIFIED)));
                fileList.add(bean);
            }
        }
        cursor.close();
        return fileList;
    }

    public static List<String> searchFiles(String Path, String Extension, String keyword, boolean IsIterative, List<String> resultList) {
        File[] files = new File(Path).listFiles();

        if (files == null)
            return null;

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isFile()) {
                String ext = Extension.toLowerCase();
                String currExt = f.getPath().substring(f.getPath().length() - Extension.length()).toLowerCase();
                if (ext.equals(currExt)) {
                    if (!StringUtils.isNullOrEmpty(keyword)) {
                        if (f.getName().indexOf(keyword) != -1) {
                            resultList.add(f.getPath());
                        }
                    } else {
                        resultList.add(f.getPath());
                    }
                }

            } else {
                if (IsIterative) {
                    if (f.isDirectory() && f.getPath().indexOf("/.") == -1)
                        resultList = searchFiles(f.getPath(), Extension, keyword, IsIterative, resultList);
                } else
                    continue;
                ;
            }
        }
        return resultList;
    }

    public static void delFiles(String Path, String Extension, String keyword, boolean IsIterative) {
        File[] files = new File(Path).listFiles();
        if (files == null)
            return;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isFile()) {
                String ext = Extension.toLowerCase();
                String currExt = f.getPath().substring(f.getPath().length() - Extension.length()).toLowerCase();
                if (ext.equals(currExt)) {
                    if (!StringUtils.isNullOrEmpty(keyword)) {
                        if (f.getName().indexOf(keyword) != -1) {
                            f.delete();
                        }
                    } else {
                        f.delete();
                    }
                }

            } else {
                if (IsIterative) {
                    if (f.isDirectory() && f.getPath().indexOf("/.") == -1)
                        delFiles(f.getPath(), Extension, keyword, IsIterative);
                } else
                    continue;
            }
        }
    }

    public static void notifySystemToScan(String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        InventoryApplication.getApplication().sendBroadcast(intent);
    }

    public static String getFileName(String pathandname) {
        if(StringUtils.isNullOrEmpty(pathandname)) return "";
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return "";
        }
    }
}
