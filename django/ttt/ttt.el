(defun ttt-run-command (what)
  (let ((compilation-buffer-name-function (lambda (mode)
                                            (format "*ttt-%s*" what))))
    (compile (format "source ~/virtualenvs/ttt/bin/activate && cd ~/src/trackthatthing/django/ttt/ && %s"
                     what))))

(defun ttt-runserver ()
  (interactive)
  (ttt-run-command "python ./manage.py runserver"))

(defvar ttt-run-history nil)

(defun ttt-completing-read-must-match (prompt choices &optional require-match
                                    initial-input history def inherit-input-method)
    "Wrapper for `helm-comp-read' that also sets :must-match to t"
    (helm-comp-read prompt
                    choices
                    :initial-input initial-input
                    :default def
                    :must-match t
                    :history history
                    ))

(defun ttt-run (cmd)
  (interactive (list (ttt-completing-read-must-match "Command to run: "
                                                     '("python ./manage.py runserver"
                                                       "python ./manage.py runserver_plus")
                                                       nil
                                                       nil
                                                       ttt-run-history)))
               (ttt-run-command (read-from-minibuffer "Command: " cmd)))

(defun ttt-goto-setting (setting)
  (let ((settings-buffer (get-buffer "settings.py")))
    (switch-to-buffer settings-buffer)
    (goto-char 0)
    (search-forward-regexp (format "\\b%s\\b"
                                   setting))
    (move-beginning-of-line nil)
    (recenter)))

(defun ttt-goto-installed-apps ()
  (interactive)
  (ttt-goto-setting "INSTALLED_APPS"))

(require 'ansi-color)

(defun colorize-compilation-buffer ()
  (toggle-read-only)
  (ansi-color-apply-on-region (point-min) (point-max))
  (toggle-read-only))

(add-hook 'compilation-filter-hook 'colorize-compilation-buffer)
