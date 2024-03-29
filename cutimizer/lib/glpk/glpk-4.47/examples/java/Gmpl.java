
import org.gnu.glpk.*;

public class Gmpl implements GlpkCallbackListener, GlpkTerminalListener {
    private boolean hookUsed = false;

    public static void main(String[] arg) {
        if (1 != arg.length) {
            System.out.println("Usage: java Gmpl model.mod");
            return;
        }
        new Gmpl().solve(arg);
    }

    public void solve(String[] arg) {
        glp_prob lp = null;
        glp_tran tran;
        glp_iocp iocp;

        String fname;
        int skip = 0;
        int ret;

        // listen to callbacks
        GlpkCallback.addListener(this);
        // listen to terminal output
        GlpkTerminal.addListener(this);

        fname = arg[0];

        lp = GLPK.glp_create_prob();
        System.out.println("Problem created");

        tran = GLPK.glp_mpl_alloc_wksp();
        ret = GLPK.glp_mpl_read_model(tran, fname, skip);
        if (ret != 0) {
            GLPK.glp_mpl_free_wksp(tran);
            GLPK.glp_delete_prob(lp);
            throw new RuntimeException("Model file not found: " + fname);
        }

        // generate model
        GLPK.glp_mpl_generate(tran, null);
        // build model
        GLPK.glp_mpl_build_prob(tran, lp);
        // set solver parameters
        iocp = new glp_iocp();
        GLPK.glp_init_iocp(iocp);
        iocp.setPresolve(GLPKConstants.GLP_ON);
        // do not listen to output anymore
        GlpkTerminal.removeListener(this);
        // solve model
        ret = GLPK.glp_intopt(lp, iocp);
        // postsolve model
        if (ret == 0) {
            GLPK.glp_mpl_postsolve(tran, lp, GLPKConstants.GLP_MIP);
        }
        // free memory
        GLPK.glp_mpl_free_wksp(tran);
        GLPK.glp_delete_prob(lp);

        // do not listen for callbacks anymore
        GlpkCallback.removeListener(this);

        // check that the hook function has been used for terminal output.
        if (!hookUsed) {
            System.out.println("Error: The terminal output hook was not used.");
            System.exit(1);
        }
    }

    public boolean output(String str) {
        hookUsed = true;
        System.out.print(str);
        return false;
    }

    public void callback(glp_tree tree) {
        int reason = GLPK.glp_ios_reason(tree);
        if (reason == GLPKConstants.GLP_IBINGO) {
            System.out.println("Better solution found");
        }
    }
}
