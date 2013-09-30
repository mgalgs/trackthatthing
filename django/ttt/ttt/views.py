from django.shortcuts import render
from django.http import HttpResponseRedirect
from django.contrib.auth.forms import UserCreationForm
from django.contrib.auth.decorators import login_required

def register(request):
    extra_message = None
    if request.method == 'POST':
        form = UserCreationForm(request.POST)
        if form.is_valid():
            newuser = form.save()
            authuser = authenticate(username=newuser.username, password=form.cleaned_data["password1"])
            if authuser is not None:
                if authuser.is_active:
                    login(request, authuser)
                    return HttpResponseRedirect('/accounts/profile/')
                else:
                    # should never happen since we just created the account
                    extra_message = 'Your account has been disabled!'
            else:
                # should never happen since we just created the account
                extra_message = 'Something crazy happened. Please email us.' + newuser.username + ' : ' + newuser.password
    else:
        form = UserCreationForm()

    return render(request, "registration/register.html", {
        'form' : form,
        'extra_message': extra_message
    })

@login_required
def profile(request):
    return render(request, 'profile.html', {
        'user': request.user,
    })
