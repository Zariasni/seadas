package gov.nasa.gsfc.seadas.processing.core;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.processing.general.*;
import gov.nasa.gsfc.seadas.processing.l2gen.productData.*;
import gov.nasa.gsfc.seadas.processing.l2gen.userInterface.*;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * A ...
 *
 * @author Danny Knowles
 * @since SeaDAS 7.0
 */
public class L2genData implements L2genDataProcessorModel {

    public static final String OPER_DIR = "l2gen";



    public static enum Mode {
        L2GEN,
        L2GEN_AQUARIUS
    }

    public static enum Source {
        L2GEN,
        RESOURCES
    }


    private static final String
            GUI_NAME = "l2gen",
            PRODUCT_INFO_XML = "productInfo.xml",
            PARAM_INFO_XML = "paramInfo.xml",
            PARAM_CATEGORY_INFO_XML = "paramCategoryInfo.xml",
            PRODUCT_CATEGORY_INFO_XML = "productCategoryInfo.xml",
            DEFAULTS_FILE_PREFIX = "msl12_defaults_",
            GETANC = "getanc.py",
            DEFAULT_SUITE = "OC";

    private static final String
            AQUARIUS_GUI_NAME = "l2gen_aquarius",
            AQUARIUS_PRODUCT_INFO_XML = "aquariusProductInfo.xml",
            AQUARIUS_PARAM_INFO_XML = "aquariusParamInfo.xml",
            AQUARIUS_PARAM_CATEGORY_INFO_XML = "aquariusParamCategoryInfo.xml",
            AQUARIUS_PRODUCT_CATEGORY_INFO_XML = "aquariusProductCategoryInfo.xml",
            AQUARIUS_DEFAULTS_FILE_PREFIX = "l2gen_aquarius_defaults_",
            AQUARIUS_GETANC = "getanc_aquarius.py",
            AQUARIUS_DEFAULT_SUITE = "V2.0";


    public static final String ANCILLARY_FILES_CATEGORY_NAME = "Ancillary Inputs";

    public static final String
            PAR = "par",
            GEOFILE = "geofile",
            SPIXL = "spixl",
            EPIXL = "epixl",
            SLINE = "sline",
            ELINE = "eline",
            NORTH = "north",
            SOUTH = "south",
            WEST = "west",
            EAST = "east",
            IFILE = "ifile",
            OFILE = "ofile",
            L2PROD = "l2prod",
            SUITE = "suite";

    public static final String
            INVALID_IFILE = "INVALID_IFILE_EVENT",
            WAVE_LIMITER = "WAVE_LIMITER_EVENT",
            RETAIN_IFILE = "RETAIN_IFILE_EVENT",
            SHOW_DEFAULTS = "SHOW_DEFAULTS_EVENT",
            PARSTRING = "PARSTRING_EVENT",
            TAB_CHANGE = "TAB_CHANGE_EVENT";


    public FileInfo iFileInfo = null;
    private boolean initialized = false;
    public boolean showIOFields = true;
    private Mode mode = Mode.L2GEN;
    private boolean  ifileIndependentMode = false;

    // keepParams: this boolean field denotes whether l2gen keeps the current params when a new ifile is selected.
    // Params not supported by the new ifile will be deleted.
     private boolean keepParams = false;


    // mode dependent fields
    private String
            paramInfoXml;
    private String productInfoXml;
    private String paramCategoryXml;
    private String productCategoryXml;
    private String getanc;
    private String defaultsFilePrefix;
    private String guiName;
    private String defaultSuite;


    public final ArrayList<L2genWavelengthInfo> waveLimiterInfos = new ArrayList<L2genWavelengthInfo>();

    private final L2genReader l2genReader = new L2genReader(this);

    private final ArrayList<ParamInfo> paramInfos = new ArrayList<ParamInfo>();


    private final ArrayList<L2genParamCategoryInfo> paramCategoryInfos = new ArrayList<L2genParamCategoryInfo>();

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    private final SeadasPrint l2genPrint = new SeadasPrint();


    // useful shortcuts to popular paramInfos
    private final HashMap<String, ParamInfo> paramInfoLookup = new HashMap<String, ParamInfo>();
    private L2genProductsParamInfo l2prodParamInfo = null;


    public boolean retainCurrentIfile = false;
    private boolean showDefaultsInParString = false;

    private ProcessorModel processorModel;


    private static L2genData me = null;

    public static L2genData getMe() {
        if (me == null) {
            me = new L2genData();
        }

        return me;

    }

    private L2genData() {
    }


    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {

        this.mode = mode;

        switch (mode) {
            case L2GEN_AQUARIUS:
                setGuiName(AQUARIUS_GUI_NAME);
                setParamInfoXml(AQUARIUS_PARAM_INFO_XML);
                setProductInfoXml(AQUARIUS_PRODUCT_INFO_XML);
                setParamCategoryXml(AQUARIUS_PARAM_CATEGORY_INFO_XML);
                setProductCategoryXml(AQUARIUS_PRODUCT_CATEGORY_INFO_XML);
                setGetanc(AQUARIUS_GETANC);
                setDefaultsFilePrefix(AQUARIUS_DEFAULTS_FILE_PREFIX);
                setDefaultSuite(AQUARIUS_DEFAULT_SUITE);
                break;
            default:
                setGuiName(GUI_NAME);
                setParamInfoXml(PARAM_INFO_XML);
                setProductInfoXml(PRODUCT_INFO_XML);
                setParamCategoryXml(PARAM_CATEGORY_INFO_XML);
                setProductCategoryXml(PRODUCT_CATEGORY_INFO_XML);
                setGetanc(GETANC);
                setDefaultsFilePrefix(DEFAULTS_FILE_PREFIX);
                setDefaultSuite(DEFAULT_SUITE);
                break;
        }

        processorModel = new ProcessorModel(getGuiName(), getParamInfos());
        processorModel.setAcceptsParFile(true);
    }

    public boolean isIfileIndependentMode() {
        return ifileIndependentMode;
    }

    public void setIfileIndependentMode(boolean ifileIndependentMode) {
        this.ifileIndependentMode = ifileIndependentMode;
    }
    public String getParamInfoXml() {
        return paramInfoXml;
    }

    public void setParamInfoXml(String paramInfoXml) {
        this.paramInfoXml = paramInfoXml;
    }

    public String getProductInfoXml() {
        return productInfoXml;
    }

    public void setProductInfoXml(String productInfoXml) {
        this.productInfoXml = productInfoXml;
    }

    public String getParamCategoryXml() {
        return paramCategoryXml;
    }

    public void setParamCategoryXml(String paramCategoryXml) {
        this.paramCategoryXml = paramCategoryXml;
    }

    public String getProductCategoryXml() {
        return productCategoryXml;
    }

    public void setProductCategoryXml(String productCategoryXml) {
        this.productCategoryXml = productCategoryXml;
    }




    public boolean isKeepParams() {
        return keepParams;
    }

    public void setKeepParams(boolean keepParams) {
        this.keepParams = keepParams;
    }



    public String getGetanc() {
        return getanc;
    }

    public void setGetanc(String getanc) {
        this.getanc = getanc;
    }

    public String getDefaultsFilePrefix() {
        return defaultsFilePrefix;
    }

    public void setDefaultsFilePrefix(String defaultsFilePrefix) {
        this.defaultsFilePrefix = defaultsFilePrefix;
    }

    public String getDefaultSuite() {
        return defaultSuite;
    }

    public void setDefaultSuite(String defaultSuite) {
        this.defaultSuite = defaultSuite;
    }

    public boolean isRetainCurrentIfile() {
        return retainCurrentIfile;
    }

    public void setRetainCurrentIfile(boolean retainCurrentIfile) {

        if (this.retainCurrentIfile != retainCurrentIfile) {
            this.retainCurrentIfile = retainCurrentIfile;
            fireEvent(RETAIN_IFILE);
        }
    }

    public boolean isMultipleInputFiles() {
        return false;
    }

    public boolean isShowDefaultsInParString() {
        return showDefaultsInParString;
    }

    public String getPrimaryInputFileOptionName() {
        return IFILE;
    }

    public String getPrimaryOutputFileOptionName() {
        return OFILE;
    }

    public void setShowDefaultsInParString(boolean showDefaultsInParString) {
        if (this.showDefaultsInParString != showDefaultsInParString) {
            this.showDefaultsInParString = showDefaultsInParString;
            fireEvent(SHOW_DEFAULTS);
        }
    }

    public boolean isValidIfile() {
        ParamInfo paramInfo = getParamInfo(IFILE);
        if (paramInfo != null) {
            return paramInfo.isValid();
        }
        return false;
    }


    public boolean isGeofileRequired() {
        switch (getMode()) {
            case L2GEN_AQUARIUS:
                return false;
            default:
                if (iFileInfo != null) {
                    return iFileInfo.isGeofileRequired();
                }

                return false;
        }
    }

    @Override
    public boolean isWavelengthRequired() {
        switch (getMode()) {
            case L2GEN_AQUARIUS:
                return false;
            default:
                return true;
        }
    }


    public EventInfo[] eventInfos = {
            new EventInfo(L2PROD, this),
            new EventInfo(PARSTRING, this)
    };


    private EventInfo getEventInfo(String name) {
        for (EventInfo eventInfo : eventInfos) {
            if (name.equals(eventInfo.getName())) {
                return eventInfo;
            }
        }
        return null;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        EventInfo eventInfo = getEventInfo(propertyName);
        if (eventInfo == null) {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        } else {
            eventInfo.addPropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        EventInfo eventInfo = getEventInfo(propertyName);
        if (eventInfo == null) {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        } else {
            eventInfo.removePropertyChangeListener(listener);
        }
    }


    public void disableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            debug("disableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(false);
        }
    }

    public void enableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            debug("enableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(true);
        }
    }

    public void fireEvent(String name) {
        fireEvent(name, null, null);
    }

    public void fireEvent(String name, Object oldValue, Object newValue) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
        } else {
            eventInfo.fireEvent(oldValue, newValue);
        }
    }

    public void fireAllParamEvents() {

        disableEvent(PARSTRING);
        disableEvent(L2PROD);

        for (ParamInfo paramInfo : paramInfos) {
            if (paramInfo.getName() != null && !paramInfo.getName().equals(L2genData.IFILE)) {
                fireEvent(paramInfo.getName());
            }
        }
        fireEvent(SHOW_DEFAULTS);
        fireEvent(RETAIN_IFILE);
        fireEvent(WAVE_LIMITER);
        fireEvent(PARSTRING);

        enableEvent(L2PROD);
        enableEvent(PARSTRING);

    }

    public void setSelectedInfo(L2genBaseInfo info, L2genBaseInfo.State state) {

        if (state != info.getState()) {
            info.setState(state);
            l2prodParamInfo.updateValue();
            fireEvent(L2PROD);
        }
    }


    /**
     * Set wavelength in waveLimiterInfos based on GUI change
     *
     * @param selectedWavelength
     * @param selected
     */
    public void setSelectedWaveLimiter(String selectedWavelength, boolean selected) {

        for (L2genWavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (selectedWavelength.equals(waveLimiterInfo.getWavelengthString())) {
                if (selected != waveLimiterInfo.isSelected()) {
                    waveLimiterInfo.setSelected(selected);
                    fireEvent(WAVE_LIMITER);
                }
            }
        }
    }


    /**
     * Determine is mission has particular waveType based on what is in the waveLimiterInfos Array
     * <p/>
     * Used by the waveLimiterInfos GUI to enable/disable the appropriate 'Select All' toggle buttons
     *
     * @param waveType
     * @return true if waveType in waveLimiterInfos, otherwise false
     */
    public boolean hasWaveType(L2genWavelengthInfo.WaveType waveType) {

        for (L2genWavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Determines if all wavelengths for a given wavelength type within the wavelength limiter array are selected
     * <p/>
     * This is used to determine whether the toggle button in the wavelength limiter GUI needs
     * to be in: 'Select All Infrared' mode, 'Deselect All Infrared' mode,
     * 'Select All Visible' mode, or 'Deselect All Visible' mode
     *
     * @return true if all of given wavelength type selected, otherwise false
     */
    public boolean isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType waveType) {

        int selectedCount = 0;

        for (L2genWavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                if (waveLimiterInfo.isSelected()) {
                    selectedCount++;
                } else {
                    return false;
                }
            }
        }

        if (selectedCount > 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Sets all wavelengths of a given wavelength type within the wavelength limiter array to selected
     * <p/>
     * This is called by the wavelength limiter GUI toggle buttons and is also used for initializing defaults.
     *
     * @param selected
     */
    public void setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType waveType, boolean selected) {

        for (L2genWavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                waveLimiterInfo.setSelected(selected);
            }
        }
        fireEvent(WAVE_LIMITER);
    }

    public void addParamInfo(ParamInfo paramInfo) {
        paramInfos.add(paramInfo);
        paramInfoLookup.put(paramInfo.getName().toLowerCase(), paramInfo);
    }

    public void clearParamInfo() {
        paramInfos.clear();
    }

    public ArrayList<ParamInfo> getParamInfos() {
        return paramInfos;
    }


    public void clearParamInfos() {
        paramInfos.clear();
    }


    public void sortParamCategoryInfos() {
        Collections.sort(paramCategoryInfos);
    }


    public void sortParamInfos() {
        Collections.sort(paramInfos);
    }


    public ArrayList<L2genWavelengthInfo> getWaveLimiterInfos() {
        return waveLimiterInfos;
    }


    /**
     * Handle cases where a change in one name should effect a change in name
     * <p/>
     * In this case specifically coordParams and pixelParams are mutually exclusive
     * so if a name in one group is being set to a non-default value, then set all
     * params in the other group to the defaults
     *
     * @param name
     */
    private void setConflictingParams(String name) {

        ParamInfo paramInfo = getParamInfo(name);
        if (paramInfo == null) {
            return;
        }

        // Only proceed if name is not equal to default
        if (paramInfo.getValue() == paramInfo.getDefaultValue()) {
            return;
        }

        // Set all params in the other group to the defaults
        final HashSet<String> coords = new HashSet<String>();
        coords.add(NORTH);
        coords.add(SOUTH);
        coords.add(EAST);
        coords.add(WEST);

        final HashSet<String> pixels = new HashSet<String>();
        pixels.add(SPIXL);
        pixels.add(EPIXL);
        pixels.add(SLINE);
        pixels.add(ELINE);

        // Test if name is coordParam
        if (coords.contains(name)) {
            for (String pixelParam : pixels) {
                setParamToDefaults(getParamInfo(pixelParam));
            }
        }

        // Set all pixelParams in paramInfos to defaults
        if (pixels.contains(name)) {
            for (String coordParam : coords) {
                setParamToDefaults(getParamInfo(coordParam));
            }
        }
    }


    public String getParString() {
        return getParString(isShowDefaultsInParString());
    }

    public String getParString(boolean showDefaults) {

        StringBuilder par = new StringBuilder("");

        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {

            boolean alwaysDisplay = false;

            StringBuilder currCategoryEntries = new StringBuilder("");

            for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
                if (paramInfo.getName().equals(IFILE)) {
                    alwaysDisplay = true;
                    currCategoryEntries.append(makeParEntry(paramInfo));
                } else if (paramInfo.getName().equals(OFILE)) {
                    alwaysDisplay = true;
                    currCategoryEntries.append(makeParEntry(paramInfo));
                } else if (paramInfo.getName().equals(GEOFILE)) {
                    if (isGeofileRequired()) {
                        currCategoryEntries.append(makeParEntry(paramInfo));
                    }
                } else if (paramInfo.getName().equals(SUITE)) {
                    alwaysDisplay = true;
                    currCategoryEntries.append(makeParEntry(paramInfo));
                } else if (paramInfo.getName().equals(PAR)) {
                    // right ignore and do not print todo
                } else {
                    if (!paramInfo.getName().startsWith("-")) {

                        if (paramInfo.getValue().equals(paramInfo.getDefaultValue())) {
                            if (showDefaults) {
                                currCategoryEntries.append(makeParEntry(paramInfo, true));
                            }
                        } else {
                            currCategoryEntries.append(makeParEntry(paramInfo));
                        }
                    }
                }
            }


            if (ANCILLARY_FILES_CATEGORY_NAME.equals(paramCategoryInfo.getName())) {
                par.append("# " + paramCategoryInfo.getName().toUpperCase() + "  Default = climatology (select 'Get Ancillary' to download ancillary files)\n");
                par.append(currCategoryEntries.toString());
                par.append("\n");
            }
            else if (currCategoryEntries.toString().length() > 0 &&  !(alwaysDisplay && !showIOFields)){
                par.append("# " + paramCategoryInfo.getName().toUpperCase() + "\n");
                par.append(currCategoryEntries.toString());
                par.append("\n");
            }
        }

        return par.toString();
    }

    private String makeParEntry(ParamInfo paramInfo) {
        return makeParEntry(paramInfo, false);
    }

    private String makeParEntry(ParamInfo paramInfo, boolean commented) {
        StringBuilder line = new StringBuilder();

        if (paramInfo.getValue().length() > 0 || paramInfo.getName().equals(L2PROD)) {
            if (commented) {
                line.append("# ");
            }

            line.append(paramInfo.getName() + "=" + paramInfo.getValue() + "\n");

            if (paramInfo.getValidationComment() != null) {
                line.append("# " + paramInfo.getValidationComment() + "\n");
            }
        }

        return line.toString();
    }


    private ArrayList<ParamInfo> parseParString(String parfileContents) {

        ArrayList<ParamInfo> paramInfos = new ArrayList<ParamInfo>();

        if (parfileContents != null) {

            String parfileLines[] = parfileContents.split("\n");

            for (String parfileLine : parfileLines) {

                // skip the comment lines in file
                if (!parfileLine.trim().startsWith("#")) {

                    String splitLine[] = parfileLine.split("=");
                    if (splitLine.length == 1 || splitLine.length == 2) {
                        String name = splitLine[0].toString().trim();
                        String value = null;

                        if (splitLine.length == 2) {
                            value = splitLine[1].toString().trim();
                        } else if (splitLine.length == 1) {
                            value = ParamInfo.NULL_STRING;
                        }

                        ParamInfo paramInfo = new ParamInfo(name, value);
                        paramInfos.add(paramInfo);
                    }
                }
            }
        }

        return paramInfos;
    }

    public void setParString(String parString, boolean ignoreIfile) {
        setParString(parString, ignoreIfile, false, null);
    }

    public void setParString(String parString, boolean ignoreIfile, boolean addParamsMode) {
        setParString(parString, ignoreIfile, addParamsMode, null);
    }

    public void setParString(String parString, boolean ignoreIfile, boolean addParamsMode, File parFileDir) {

        // addParamsMode is a special mode.
        // this enables a subset parstring to be sent in
        // essentially this means that you are sending in a list of params to change but leaving anything else untouched

        disableEvent(PARSTRING);

        // keepParams variable is tied to selection of ifile by the selector
        // this variable does not apply for this case where the params are being set be a parString
        // so disable keepParams and then later put it back to the original value
        boolean tmpKeepParams = isKeepParams();
        setKeepParams(false);

        ArrayList<ParamInfo> parfileParamInfos = parseParString(parString);

        String ifile = null;
        String suite = null;

        /*
        Handle IFILE and SUITE first
         */
        if (!ignoreIfile) {
            for (ParamInfo parfileParamInfo : parfileParamInfos) {
                if (parfileParamInfo.getName().toLowerCase().equals(IFILE)) {
                    File tmpFile = SeadasFileUtils.createFile(parFileDir, parfileParamInfo.getValue());
                    ifile = tmpFile.getAbsolutePath();
                    break;
                }
            }
        }

        for (ParamInfo parfileParamInfo : parfileParamInfos) {
            if (parfileParamInfo.getName().toLowerCase().equals(SUITE)) {
                suite = parfileParamInfo.getValue();
                break;
            }
        }

        if ((!ignoreIfile && ifile != null) || suite != null) {
            setIfileAndSuiteParamValues(ifile, suite);
        }


        /*
       Handle L2PROD
        */

        ArrayList<String> l2prods = null;

        for (ParamInfo test : parfileParamInfos) {
            if (test.getName().toLowerCase().startsWith(L2PROD)) {
                if (l2prods == null) {
                    l2prods = new ArrayList<String>();
                }

                l2prods.add(test.getValue());
            }
        }

        if (l2prods != null) {
            StringUtils.join(l2prods, " ");
            setParamValue(L2PROD, StringUtils.join(l2prods, " "));
        }


        /*
       Set all params contained in parString
       Ignore IFILE (handled earlier) and PAR (which is todo)
        */
        for (ParamInfo newParamInfo : parfileParamInfos) {


            if (newParamInfo.getName().toLowerCase().equals(OFILE) && ignoreIfile) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(GEOFILE) && ignoreIfile) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(IFILE)) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(PAR)) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().startsWith(L2PROD)) {
                continue;
            }

            setParamValue(newParamInfo.getName(), newParamInfo.getValue());
        }


        if (!addParamsMode) {

            /*
           Delete all params NOT contained in parString to defaults (basically set to default)
           Except: L2PROD and IFILE  remain at current value
            */
            for (ParamInfo paramInfo : paramInfos) {
                if (!paramInfo.getName().startsWith(L2PROD) && !paramInfo.getName().equals(IFILE) && !paramInfo.getName().equals(OFILE) && !paramInfo.getName().equals(GEOFILE)) {
                    boolean paramHandled = false;
                    for (ParamInfo parfileParamInfo : parfileParamInfos) {
                        if (paramInfo.getName().toLowerCase().equals(parfileParamInfo.getName().toLowerCase())) {
                            paramHandled = true;
                        }
                    }

                    if (!paramHandled && (paramInfo.getValue() != paramInfo.getDefaultValue())) {
                        setParamValue(paramInfo.getName(), paramInfo.getDefaultValue());
                    }
                }
            }

        }

        fireEvent(PARSTRING);
        enableEvent(PARSTRING);

        setKeepParams(tmpKeepParams);
    }

    public boolean hasParamValue(String name) {
        return paramInfoLookup.containsKey(name);
    }

    public ParamInfo getParamInfo(String name) {

        if (name == null) {
            return null;
        }

        name = name.trim().toLowerCase();

        return paramInfoLookup.get(name);
    }


    private String getParamValue(ParamInfo paramInfo) {
        if (paramInfo == null)
            return null;
        return paramInfo.getValue();
    }


    public String getParamValue(String name) {
        return getParamValue(getParamInfo(name));
    }


    private boolean getBooleanParamValue(ParamInfo paramInfo) {
        if (paramInfo.getValue().equals(ParamInfo.BOOLEAN_TRUE)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getBooleanParamValue(String name) {
        return getBooleanParamValue(getParamInfo(name));
    }


    private File getParamFile(ParamInfo paramInfo) {
        if (paramInfo != null && iFileInfo != null) {
            return paramInfo.getFile(iFileInfo.getFile().getParentFile());
        }
        return null;
    }

    public File getParamFile(String name) {
        return getParamFile(getParamInfo(name));
    }


    public void setParamValueAndDefault(String name, String value) {
        setParamValueAndDefault(getParamInfo(name), value);
    }


    public void setParamValueAndDefault(ParamInfo paramInfo, String value) {
        if (paramInfo == null) {
            return;
        }
        if (value == null) {
            value = ParamInfo.NULL_STRING;
        }

        if (!value.equals(paramInfo.getValue()) || !value.equals(paramInfo.getDefaultValue())) {
            if (paramInfo.getName().toLowerCase().equals(IFILE)) {
                setIfileParamValue(paramInfo, value);
                paramInfo.setDefaultValue(paramInfo.getValue());
            } else {
                paramInfo.setValue(value);
                paramInfo.setDefaultValue(paramInfo.getValue());
                setConflictingParams(paramInfo.getName());
                if (paramInfo.getType() == ParamInfo.Type.IFILE) {
                    paramInfo.validateIfileValue(null, SeadasProcessorInfo.Id.L2GEN);
                }
                fireEvent(paramInfo.getName());
            }
        }
    }


    private void setParamValue(ParamInfo paramInfo, String value) {
        if (paramInfo == null) {
            return;
        }
        if (value == null) {
            value = ParamInfo.NULL_STRING;
        }

        if (!value.equals(paramInfo.getValue())) {


            if (paramInfo.getName().toLowerCase().equals(IFILE)) {

//todo Danny is working on this in order to maintain existing params when new ifile is selected
                String tmpParString = null;

                if (isKeepParams()) {
                    tmpParString = getParString();
                }

                if (getMode() == Mode.L2GEN_AQUARIUS) {
                    setIfileAndSuiteParamValues(value, getDefaultSuite());
                } else {
                    setIfileParamValue(paramInfo, value);
                }

                if (isKeepParams()) {
                    setParString(tmpParString, true, true);
                }

            } else if (paramInfo.getName().toLowerCase().equals(SUITE)) {
                setIfileAndSuiteParamValues(null, value);
            } else {
                if (value.length() > 0 || paramInfo.getName().toLowerCase().equals(L2PROD)) {

                    paramInfo.setValue(value);

                    if (paramInfo.getType() == ParamInfo.Type.IFILE) {
                        paramInfo.validateIfileValue(iFileInfo.getFile().getParent(), SeadasProcessorInfo.Id.L2GEN);
                    }
                    setConflictingParams(paramInfo.getName());
                } else {
                    paramInfo.setValue(paramInfo.getDefaultValue());
                }
                fireEvent(paramInfo.getName());
            }

        }
    }


    public void setParamValue(String name, String value) {
        setParamValue(getParamInfo(name), value);
    }


    private void setIfileAndSuiteParamValues(String ifileValue, String suiteValue) {
        setIfileandSuiteParamValuesWrapper(getParamInfo(IFILE), ifileValue, suiteValue);
    }

    private void setParamValue(ParamInfo paramInfo, boolean selected) {
        if (selected) {
            setParamValue(paramInfo, ParamInfo.BOOLEAN_TRUE);
        } else {
            setParamValue(paramInfo, ParamInfo.BOOLEAN_FALSE);
        }
    }

    public void setParamValue(String name, boolean selected) {
        setParamValue(getParamInfo(name), selected);
    }


    private void setParamValue(ParamInfo paramInfo, ParamValidValueInfo paramValidValueInfo) {
        setParamValue(paramInfo, paramValidValueInfo.getValue());
    }


    public void setParamValue(String name, ParamValidValueInfo paramValidValueInfo) {
        setParamValue(getParamInfo(name), paramValidValueInfo);
    }

    private boolean isParamDefault(ParamInfo paramInfo) {
        if (paramInfo.getValue().equals(paramInfo.getDefaultValue())) {
            return true;
        } else {
            return false;
        }
    }


    public boolean isParamDefault(String name) {
        return isParamDefault(getParamInfo(name));
    }


    private String getParamDefault(ParamInfo paramInfo) {
        if (paramInfo != null) {
            return paramInfo.getDefaultValue();
        } else {
            return null;
        }
    }

    public String getParamDefault(String name) {
        return getParamDefault(getParamInfo(name));
    }


    private void setParamToDefaults(ParamInfo paramInfo) {
        if (paramInfo != null) {
            setParamValue(paramInfo, paramInfo.getDefaultValue());
        }
    }


    public void setParamToDefaults(String name) {
        setParamToDefaults(getParamInfo(name));
    }

    public void setToDefaults(L2genParamCategoryInfo paramCategoryInfo) {
        for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
            setParamToDefaults(paramInfo);
        }
    }


    public boolean isParamCategoryDefault(L2genParamCategoryInfo paramCategoryInfo) {
        boolean isDefault = true;

        for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
            if (!paramInfo.isDefault()) {
                isDefault = false;
            }
        }

        return isDefault;
    }


    private File getSensorInfoFilename() {

        getSuiteList();

        if (iFileInfo != null && iFileInfo.getMissionDirectory() != null) {
            // determine the filename which contains the wavelengths
            File filename = new File(iFileInfo.getMissionDirectory().getAbsolutePath(), "msl12_sensor_info.dat");
            return filename;

        } else {
            return null;
        }
    }


    public String[] getSuiteList() {

        if (iFileInfo != null && iFileInfo.getMissionDirectory() != null) {

            ArrayList<String> suitesArrayList = new ArrayList<String>();

            File missionDirectoryFiles[] = iFileInfo.getMissionDirectory().listFiles();

            for (File file : missionDirectoryFiles) {
                String filename = file.getName();

                if (filename.startsWith(getDefaultsFilePrefix()) && filename.endsWith(".par")) {
                    String filenameTrimmed = filename.replaceFirst(getDefaultsFilePrefix(), "");
                    filenameTrimmed = filenameTrimmed.replaceAll("[\\.][p][a][r]$", "");
                    suitesArrayList.add(filenameTrimmed);
                }
            }

            final String[] suitesArray = new String[suitesArrayList.size()];

            int i = 0;
            for (String suite : suitesArrayList) {
                suitesArray[i] = suite;
                i++;
            }

            return suitesArray;

        } else {
            return null;
        }
    }


    private void resetWaveLimiter() {
        waveLimiterInfos.clear();

        if (L2genData.getMe().isIfileIndependentMode()) {
            L2genWavelengthInfo wavelengthInfo = new L2genWavelengthInfo(L2genProductTools.WAVELENGTH_FOR_IFILE_INDEPENDENT_MODE);
            waveLimiterInfos.add(wavelengthInfo);
            return;
        }
        // determine the filename which contains the wavelengths
        File sensorInfoFilename = getSensorInfoFilename();

        if (sensorInfoFilename != null) {
            // read in the mission's datafile which contains the wavelengths
            //  final ArrayList<String> SensorInfoArrayList = myReadDataFile(sensorInfoFilename.toString());
            final ArrayList<String> SensorInfoArrayList = l2genReader.readFileIntoArrayList(sensorInfoFilename);
            debug("sensorInfoFilename=" + sensorInfoFilename);


            // loop through datafile
            for (String myLine : SensorInfoArrayList) {

                // skip the comment lines in file
                if (!myLine.trim().startsWith("#")) {

                    // just look at value pairs of the form Lambda(#) = #
                    String splitLine[] = myLine.split("=");
                    if (splitLine.length == 2 &&
                            splitLine[0].trim().startsWith("Lambda(") &&
                            splitLine[0].trim().endsWith(")")
                            ) {

                        // get current wavelength and add into in a JCheckBox
                        final String currWavelength = splitLine[1].trim();

                        L2genWavelengthInfo wavelengthInfo = new L2genWavelengthInfo(currWavelength);
                        waveLimiterInfos.add(wavelengthInfo);
                        debug("wavelengthLimiterArray adding wave=" + wavelengthInfo.getWavelengthString());
                    }
                }
            }
        }
    }


    // runs this if IFILE changes
    // it will reset missionString
    // it will reset and make new wavelengthInfoArray


    private void setIfileParamValue(ParamInfo paramInfo, String value) {
        setIfileandSuiteParamValuesWrapper(paramInfo, value, null);
    }


    private void setIfileandSuiteParamValuesWrapper(final ParamInfo ifileParamInfo, final String ifileValue, final String suiteValue) {

        String currIfile = getParamValue(IFILE);
        String currSuite = getParamValue(SUITE);
        if (currIfile != null && currSuite != null) {
            if (currIfile.equals(ifileValue) && currSuite.equals(suiteValue)) {
                return;
            }
        }

        if (isInitialized()) {
            VisatApp visatApp = VisatApp.getApp();
            ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(visatApp.getMainFrame(),
                    GUI_NAME) {

                @Override
                protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                    pm.beginTask("Re-initializing " + GUI_NAME, 2);

                    setIfileandSuiteParamValues(ifileParamInfo, ifileValue, suiteValue);
                    pm.done();

                    return null;
                }


            };

            worker.executeWithBlocking();

            try {
                worker.get();
            } catch (Exception e) {

            }
        } else {
            setIfileandSuiteParamValues(ifileParamInfo, ifileValue, suiteValue);
        }


    }


    private void setIfileandSuiteParamValues(ParamInfo ifileParamInfo, String ifileValue, String suiteValue) {

        if (ifileParamInfo == null) {
            return;
        }

        if (ifileValue == null) {
            ifileValue = ifileParamInfo.getValue();
            if (ifileValue == null) {
                return;
            }
        }

        disableEvent(PARSTRING);
        disableEvent(L2PROD);

        String oldIfile = getParamValue(getParamInfo(IFILE));

        ifileParamInfo.setValue(ifileValue);
//        ifileParamInfo.setDefaultValue(ifileValue);

        iFileInfo = ifileParamInfo.validateIfileValue(null, SeadasProcessorInfo.Id.L2GEN);
        processorModel.setReadyToRun(isValidIfile());

        if (iFileInfo != null && isValidIfile()) {

            //  todo temporary more Joel updates
            if (suiteValue != null) {
                getParamInfo(SUITE).setValue(suiteValue);
            }


            if (iFileInfo.getMissionId() == MissionInfo.Id.AQUARIUS || iFileInfo.getMissionId() == MissionInfo.Id.VIIRS) {
                updateLuts(iFileInfo.getMissionDirectory().getAbsolutePath());
            }

            resetWaveLimiter();
            l2prodParamInfo.resetProductInfos();
            try {
                updateXmlBasedObjects(iFileInfo.getFile(), suiteValue);
            } catch (IOException e) {

                SimpleDialogMessage dialog = new SimpleDialogMessage(null, "ERROR: " + e.getMessage());
                dialog.setVisible(true);
                dialog.setEnabled(true);
            }


            FileInfo oFileInfo;

            if (iFileInfo.isMissionId(MissionInfo.Id.AQUARIUS)) {
                oFileInfo = FilenamePatterns.getAquariusOFileInfo(iFileInfo, suiteValue);
            } else {
                oFileInfo = FilenamePatterns.getOFileInfo(iFileInfo);
            }
            if (oFileInfo != null) {
                setParamValue(OFILE, oFileInfo.getFile().getAbsolutePath());
            }

            if (iFileInfo.isGeofileRequired()) {
                FileInfo geoFileInfo = FilenamePatterns.getGeoFileInfo(iFileInfo);
                if (geoFileInfo != null) {
                    setParamValue(GEOFILE, geoFileInfo.getFile().getAbsolutePath());
                }
            } else {
                setParamValueAndDefault(GEOFILE, null);
            }
        } else {
            setParamToDefaults(OFILE);
            setParamValueAndDefault(GEOFILE, null);
            fireEvent(INVALID_IFILE);
        }


        setParamValueAndDefault(PAR, ParamInfo.NULL_STRING);


        fireEvent(IFILE, oldIfile, ifileValue);

        fireEvent(PARSTRING);
        enableEvent(L2PROD);
        enableEvent(PARSTRING);

    }


    public void updateLuts(String missionName) {

        String UPDATE_LUTS_SCRIPT = "update_luts.py";

        ProcessorModel processorModel = new ProcessorModel(UPDATE_LUTS_SCRIPT);
        processorModel.setAcceptsParFile(false);

        processorModel.addParamInfo("mission", missionName, ParamInfo.Type.STRING, 0);

        try {
            Process p = OCSSWRunner.execute(processorModel.getProgramCmdArray(), processorModel.getIFileDir()); //processorModel.executeProcess();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = stdInput.readLine();

        } catch (IOException e) {
            System.out.println("ERROR - Problem running " + UPDATE_LUTS_SCRIPT);
            System.out.println(e.getMessage());
            return;
        }

    }


    public void setAncillaryFiles(boolean refreshDB, boolean forceDownload, boolean getNO2) {
        //   getanc.py --refreshDB <FILE>

        if (!isValidIfile()) {
            System.out.println("ERROR - Can not run getanc.py without a valid ifile.");
            return;
        }

        // get the ifile
        final String ifile = getParamValue(getParamInfo(IFILE));
        final StringBuilder ancillaryFiles = new StringBuilder("");

        final ProcessorModel processorModel = new ProcessorModel(getGetanc());
        processorModel.setAcceptsParFile(false);

        //position is changed from 1 to 0.
        int position = 0;
        if (refreshDB) {
            processorModel.addParamInfo("refreshDB", "--refreshDB", ParamInfo.Type.STRING, position);
            position++;
        }
        if (forceDownload) {
            processorModel.addParamInfo("force-download", "--force-download", ParamInfo.Type.STRING, position);
            position++;
        }
        if (getNO2) {
            processorModel.addParamInfo("no2", "--no2", ParamInfo.Type.STRING, position);
            position++;
        }

        processorModel.addParamInfo("ifile", ifile, ParamInfo.Type.IFILE, position);

        final File iFile = new File(ifile);


        VisatApp visatApp = VisatApp.getApp();
        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(visatApp.getMainFrame(),
                "GetAnc") {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                pm.beginTask("Retrieving ancillary files", 2);

                try {
                    Process p = OCSSWRunner.execute(processorModel.getProgramCmdArray(), processorModel.getIFileDir()); //processorModel.executeProcess();

                    // Determine exploded filenames
                    File runDirectoryFiles[] = processorModel.getIFileDir().listFiles();

                    for (File file : runDirectoryFiles) {
                        if (file.getName().startsWith(iFile.getName().substring(0, 13))) {
                            if (file.getName().endsWith(".txt") || file.getName().endsWith(".anc")) {
                                file.deleteOnExit();
                            }
                        }
                    }


                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    String line = stdInput.readLine();
                    while (line != null) {
                        if (line.contains("=")) {
                            ancillaryFiles.append(line);
                            ancillaryFiles.append("\n");

                            // Delete all ancillary files in operational (IFILE) directory on program exit
                            String[] splitLine = line.split("=");
                            if (splitLine.length == 2) {
                                File currentFile = new File(splitLine[1]);
                                if (currentFile.isAbsolute()) {
                                    if (currentFile.getParent() != null && currentFile.getParent().equals(iFile.getParent())) {
                                        currentFile.deleteOnExit();
                                    }
                                } else {
                                    File absoluteCurrentFile = new File(processorModel.getIFileDir().getAbsolutePath(), currentFile.getName());
                                    absoluteCurrentFile.deleteOnExit();
                                }
                            }
                        }
                        line = stdInput.readLine();
                    }

                    pm.worked(1);

                } catch (IOException e) {
                    pm.done();
                    SimpleDialogMessage dialog = new SimpleDialogMessage(null, "ERROR - Problem running " + getGetanc() + " " + e.getMessage());
                    dialog.setVisible(true);
                    dialog.setEnabled(true);


                } finally {
                    pm.done();
                }
                return null;
            }


        };

        pmSwingWorker.executeWithBlocking();


        setParString(ancillaryFiles.toString(), true, true);

    }

//    public void setAncillaryFiles() {
//
//        if (!isValidIfile()) {
//            System.out.println("ERROR - Can not run getanc.py without a valid ifile.");
//            return;
//        }
//
//        // get the ifile
//        String ifile = getParamValue(getParamInfo(IFILE));
//        StringBuilder ancillaryFiles = new StringBuilder("");
//
//        ProcessorModel processorModel = new ProcessorModel("getanc.py");
//        processorModel.setAcceptsParFile(false);
//        processorModel.addParamInfo("ifile", ifile, ParamInfo.Type.IFILE, 1);
//
//        try {
//            Process p = OCSSWRunner.execute(processorModel.getProgramCmdArray(), processorModel.getIFileDir()); //processorModel.executeProcess();
//            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//            String line = stdInput.readLine();
//            while (line != null) {
//                if (line.contains("=")) {
//                    ancillaryFiles.append(line);
//                    ancillaryFiles.append("\n");
//                }
//                line = stdInput.readLine();
//            }
//        } catch (IOException e) {
//            System.out.println("ERROR - Problem running getanc.py");
//            System.out.println(e.getMessage());
//            return;
//        }
//
//        setParString(ancillaryFiles.toString(), true, true);
//    }
//

    private void debug(String string) {

        //  System.out.println(string);
    }


    /**
     * resets paramInfos within paramCategoryInfos to link to appropriate entry in paramInfos
     */
    public void setParamCategoryInfos() {
        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
            paramCategoryInfo.clearParamInfos();
        }

        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
            for (String categorizedParamName : paramCategoryInfo.getParamNames()) {
                for (ParamInfo paramInfo : paramInfos) {
                    if (categorizedParamName.equals(paramInfo.getName())) {
                        paramCategoryInfo.addParamInfos(paramInfo);
                    }
                }
            }
        }


        for (ParamInfo paramInfo : paramInfos) {
            boolean found = false;

            for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
                for (String categorizedParamName : paramCategoryInfo.getParamNames()) {
                    if (categorizedParamName.equals(paramInfo.getName())) {
                        //  paramCategoryInfo.addParamInfos(paramInfo);
                        found = true;
                    }
                }
            }

            if (!found) {
                for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
                    if (paramCategoryInfo.isDefaultBucket()) {
                        paramCategoryInfo.addParamInfos(paramInfo);
                        l2genPrint.adminlog("Dropping uncategorized param '" + paramInfo.getName() + "' into the defaultBucket");
                    }
                }
            }
        }


    }


    public boolean compareWavelengthLimiter(L2genWavelengthInfo wavelengthInfo) {
        for (L2genWavelengthInfo waveLimitorInfo : getWaveLimiterInfos()) {
            if (waveLimitorInfo.getWavelength() == wavelengthInfo.getWavelength()) {
                if (waveLimitorInfo.isSelected()) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public ArrayList<L2genParamCategoryInfo> getParamCategoryInfos() {
        return paramCategoryInfos;
    }


    public void addParamCategoryInfo(L2genParamCategoryInfo paramCategoryInfo) {
        paramCategoryInfos.add(paramCategoryInfo);
    }

    public void clearParamCategoryInfos() {
        paramCategoryInfos.clear();
    }


    private void updateXmlBasedObjects(File iFile) throws IOException {
        updateXmlBasedObjects(iFile, null);
    }

    private void updateXmlBasedObjects(File iFile, String suite) throws IOException {

        InputStream paramInfoStream = getParamInfoInputStream(iFile, suite);

        // do this to create new productInfoXml file which is only used next time SeaDAS is run
        switch (getMode()) {
            case L2GEN_AQUARIUS:
                getProductInfoInputStream(Source.RESOURCES, false);
                break;
            default:
                getProductInfoInputStream(Source.L2GEN, true);
                break;
        }

        // will only update existing params; next time SeaDAS is run the new params will show up
        l2genReader.updateParamInfosWithXml(paramInfoStream);
    }


    private InputStream getProductInfoInputStream(Source source, boolean overwrite) throws IOException {
        File dataDir = SystemUtils.getApplicationDataDir();
        File l2genDir = new File(dataDir, OPER_DIR);
        l2genDir.mkdirs();

        File xmlFile = new File(l2genDir, getProductInfoXml());

        if (source == Source.RESOURCES) {
            if (!xmlFile.exists() || overwrite) {
                xmlFile = installResource(getProductInfoXml());
            }

            if (xmlFile == null || !xmlFile.exists()) {
                throw new IOException("product XML file does not exist");
            }

            try {
                return new FileInputStream(xmlFile);
            } catch (IOException e) {
                throw new IOException("problem creating product XML file: " + e.getMessage());
            }
        } else if (source == Source.L2GEN) {
            if (!xmlFile.exists() || overwrite) {
                ProcessorModel processorModel = new ProcessorModel(getGuiName());
                processorModel.setAcceptsParFile(false);
                processorModel.addParamInfo("prodxmlfile", xmlFile.getAbsolutePath(), ParamInfo.Type.OFILE);
                processorModel.getParamInfo("prodxmlfile").setUsedAs(ParamInfo.USED_IN_COMMAND_AS_OPTION);

                try {
                    String[] programCommand = processorModel.getProgramCmdArray();

                    Process p = OCSSWRunner.execute(programCommand, processorModel.getIFileDir());
                    p.waitFor();

                    if (p.exitValue() != 0) {
                        throw new IOException(getGuiName() + " returned nonzero exitvalue");
                    }

                    if (!xmlFile.exists()) {
                        return null;
                    }

                    return new FileInputStream(xmlFile);
                } catch (IOException e) {
                    throw new IOException("Problem creating product XML file: " + e.getMessage());
                } catch (InterruptedException e) {
                    throw new IOException("Problem waiting for l2gen for Product XML file: " + e.getMessage());
                }
            }
        }

        return null;
    }


    private InputStream getParamInfoInputStream() throws IOException {

        File dataDir = SystemUtils.getApplicationDataDir();
        File l2genDir = new File(dataDir, OPER_DIR);
        l2genDir.mkdirs();

        File xmlFile = new File(l2genDir, getParamInfoXml());

        if (!xmlFile.exists()) {
            xmlFile = installResource(getParamInfoXml());
        }

        if (xmlFile == null || !xmlFile.exists()) {
            throw new IOException("param XML file does not exist");
        }

        try {
            return new FileInputStream(xmlFile);
        } catch (IOException e) {
            throw new IOException("problem creating param XML file: " + e.getMessage());
        }
    }


    private InputStream getParamInfoInputStream(File file, String suite) throws IOException {

        File dataDir = SystemUtils.getApplicationDataDir();
        File l2genDir = new File(dataDir, OPER_DIR);

        l2genDir.mkdirs();
        File xmlFile;

        xmlFile = new File(l2genDir, getParamInfoXml());
        ProcessorModel processorModel = new ProcessorModel(getGuiName());

        processorModel.setAcceptsParFile(true);
        processorModel.addParamInfo("ifile", file.getAbsolutePath(), ParamInfo.Type.IFILE);

        if (suite != null) {
            processorModel.addParamInfo("suite", suite, ParamInfo.Type.STRING);
        }

        processorModel.addParamInfo("-dump_options_xmlfile", xmlFile.getAbsolutePath(), ParamInfo.Type.OFILE);

        try {
            String[] programCommand = processorModel.getProgramCmdArray();
            Process p = OCSSWRunner.execute(programCommand, processorModel.getIFileDir());//processorModel.executeProcess();
            p.waitFor();
            if (p.exitValue() != 0) {
                throw new IOException("l2gen failed to run");
            }

            if (!xmlFile.exists()) {
                return null;
            }

            return new FileInputStream(xmlFile);
        } catch (IOException e) {
            throw new IOException("problem creating Parameter XML file: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new IOException("problem waiting for l2gen for Parameter XML file: " + e.getMessage());
        }
    }


    public void setInitialValues(File iFile) {
        ParamInfo ifileParamInfo = getParamInfo(IFILE);
        setParamValueAndDefault(ifileParamInfo, ParamInfo.NULL_STRING);
        if (iFile != null) {
            setParamValue(ifileParamInfo, iFile.toString());

            // reset suite and ifile to same value because this fires needed GUI updates events
//            if (iFileInfo.isMissionId(MissionInfo.Id.AQUARIUS)) {
//                setIfileAndSuiteParamValues(iFile.getAbsolutePath(), getParamValue(SUITE));
//            }
        }
    }


    public StatusInfo initXmlBasedObjects() throws IOException {

        StatusInfo myStatusInfo = new StatusInfo(StatusInfo.Id.SUCCEED);

        InputStream paramInfoStream = getParamInfoInputStream();

        if (paramInfoStream != null) {
            disableEvent(PARSTRING);
            disableEvent(L2PROD);

            l2genReader.readParamInfoXml(paramInfoStream);
            processorModel.setParamList(paramInfos);

            InputStream paramCategoryInfoStream = L2genForm.class.getResourceAsStream(getParamCategoryXml());
            l2genReader.readParamCategoryXml(paramCategoryInfoStream);
            setParamCategoryInfos();

            fireEvent(PARSTRING);
            enableEvent(L2PROD);
            enableEvent(PARSTRING);

            return myStatusInfo;
        }


        myStatusInfo.setStatus(StatusInfo.Id.FAIL);
        myStatusInfo.setMessage("Failed");
        return myStatusInfo;
    }


    public void setL2prodParamInfo(L2genProductsParamInfo l2prodParamInfo) {
        this.l2prodParamInfo = l2prodParamInfo;
    }


    public void addProductInfo(L2genProductInfo productInfo) {
        l2prodParamInfo.addProductInfo(productInfo);
    }


    public void clearProductInfos() {
        l2prodParamInfo.clearProductInfos();
    }


    public void sortProductInfos(Comparator<L2genProductInfo> comparator) {
        l2prodParamInfo.sortProductInfos(comparator);
    }

    public void setProdToDefault() {
        if (!l2prodParamInfo.isDefault()) {
            l2prodParamInfo.setValue(l2prodParamInfo.getDefaultValue());
            fireEvent(L2PROD);
        }
    }


    /**
     * resets productInfos within productCategoryInfos to link to appropriate entry in productInfos
     */
    public void setProductCategoryInfos() {
        l2prodParamInfo.setProductCategoryInfos();
    }

    public ArrayList<L2genProductCategoryInfo> getProductCategoryInfos() {
        return l2prodParamInfo.getProductCategoryInfos();
    }

    public void addProductCategoryInfo(L2genProductCategoryInfo productCategoryInfo) {
        l2prodParamInfo.addProductCategoryInfo(productCategoryInfo);
    }

    public void clearProductCategoryInfos() {
        l2prodParamInfo.clearProductCategoryInfos();
    }


    public L2genProductsParamInfo createL2prodParamInfo(String value) throws IOException {

        InputStream productInfoStream;

        switch (getMode()) {
            case L2GEN_AQUARIUS:
                productInfoStream = getProductInfoInputStream(Source.RESOURCES, false);
                break;
            default:
                productInfoStream = getProductInfoInputStream(Source.L2GEN, true);
                break;
        }


        L2genProductsParamInfo l2prodParamInfo = new L2genProductsParamInfo();
        setL2prodParamInfo(l2prodParamInfo);

        l2genReader.readProductsXml(productInfoStream);

        l2prodParamInfo.setValue(value);

        InputStream productCategoryInfoStream = L2genForm.class.getResourceAsStream(getProductCategoryXml());
        l2genReader.readProductCategoryXml(productCategoryInfoStream);
        setProductCategoryInfos();

        return l2prodParamInfo;

    }


    public String sortStringList(String stringlist) {
        String[] products = stringlist.split("\\s+");
        ArrayList<String> productArrayList = new ArrayList<String>();
        for (String product : products) {
            productArrayList.add(product);
        }
        Collections.sort(productArrayList);

        return StringUtils.join(productArrayList, " ");
    }


    public static File installResource(final String fileName) {
        final File dataDir = new File(SystemUtils.getApplicationDataDir(), OPER_DIR);
        File theFile = new File(dataDir, fileName);
        if (theFile.canRead()) {
            return theFile;
        }
        final URL codeSourceUrl = L2genData.class.getProtectionDomain().getCodeSource().getLocation();
        final ResourceInstaller resourceInstaller = new ResourceInstaller(codeSourceUrl, "gov/nasa/gsfc/seadas/processing/l2gen/userInterface/",
                dataDir);

        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(VisatApp.getApp().getApplicationWindow(),
                "Installing Auxdata...") {
            @Override
            protected Object doInBackground(ProgressMonitor progressMonitor) throws Exception {
                resourceInstaller.install(fileName, progressMonitor);
                return Boolean.TRUE;
            }

            /**
             * Executed on the <i>Event Dispatch Thread</i> after the {@code doInBackground}
             * method is finished. The default
             * implementation does nothing. Subclasses may override this method to
             * perform completion actions on the <i>Event Dispatch Thread</i>. Note
             * that you can query status inside the implementation of this method to
             * determine the result of this task or whether this task has been cancelled.
             *
             * @see #doInBackground
             * @see #isCancelled()
             * @see #get
             */
            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    VisatApp.getApp().getLogger().log(Level.SEVERE, "Could not install tiny iFile", e);
                }
            }
        };

        swingWorker.executeWithBlocking();
        return theFile;
    }

    public ProcessorModel getProcessorModel() {
        processorModel.setReadyToRun(isValidIfile());
        return processorModel;
    }

    public void updateParamValues(Product selectedProduct) {

    }


    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getGuiName() {
        return guiName;
    }

    public void setGuiName(String guiName) {
        this.guiName = guiName;
    }
}
